#!/bin/bash

set -e

echo "========== Initializing LocalStack =========="

###############################################
# S3
###############################################

echo "Creating S3 bucket..."

awslocal s3 mb s3://syncbeat-audio || true

###############################################
# SNS FIFO Topic
###############################################

echo "Creating SNS topic..."

TOPIC_ARN=$(awslocal sns create-topic \
    --name room-events-topic.fifo \
    --attributes FifoTopic=true,ContentBasedDeduplication=true \
    --query TopicArn \
    --output text)

###############################################
# SQS FIFO Queues
###############################################

echo "Creating Sync Queue..."

SYNC_QUEUE_URL=$(awslocal sqs create-queue \
    --queue-name sync-queue.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=true \
    --query QueueUrl \
    --output text)

echo "Creating Analytics Queue..."

ANALYTICS_QUEUE_URL=$(awslocal sqs create-queue \
    --queue-name analytics-queue.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=true \
    --query QueueUrl \
    --output text)

echo "Creating History Queue..."

HISTORY_QUEUE_URL=$(awslocal sqs create-queue \
    --queue-name activity-log-queue.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=true \
    --query QueueUrl \
    --output text)

###############################################
# DLQs
###############################################

SYNC_DLQ_URL=$(awslocal sqs create-queue \
    --queue-name sync-queue-dlq.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=true \
    --query QueueUrl \
    --output text)

ANALYTICS_DLQ_URL=$(awslocal sqs create-queue \
    --queue-name analytics-queue-dlq.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=true \
    --query QueueUrl \
    --output text)

HISTORY_DLQ_URL=$(awslocal sqs create-queue \
    --queue-name activity-log-queue-dlq.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=true \
    --query QueueUrl \
    --output text)

###############################################
# Queue ARNs
###############################################

SYNC_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url "$SYNC_QUEUE_URL" \
    --attribute-names QueueArn \
    --query Attributes.QueueArn \
    --output text)

ANALYTICS_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url "$ANALYTICS_QUEUE_URL" \
    --attribute-names QueueArn \
    --query Attributes.QueueArn \
    --output text)

HISTORY_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url "$HISTORY_QUEUE_URL" \
    --attribute-names QueueArn \
    --query Attributes.QueueArn \
    --output text)

###############################################
# Queue Policies
###############################################

create_policy() {
QUEUE_URL=$1
QUEUE_ARN=$2
ATTR_FILE=$(mktemp)

# Built with python3 rather than hand-escaped bash strings: a multi-line
# bash string embedded inside a quoted JSON value produces literal
# newline control characters, which is invalid JSON and was silently
# breaking every set-queue-attributes call.
python3 - "$QUEUE_ARN" "$TOPIC_ARN" "$ATTR_FILE" <<'PYEOF'
import json, sys

queue_arn, topic_arn, out_path = sys.argv[1], sys.argv[2], sys.argv[3]

policy = {
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {"AWS": "*"},
            "Action": "SQS:SendMessage",
            "Resource": queue_arn,
            "Condition": {"ArnEquals": {"aws:SourceArn": topic_arn}},
        }
    ],
}

with open(out_path, "w") as f:
    json.dump({"Policy": json.dumps(policy)}, f)
PYEOF

awslocal sqs set-queue-attributes \
    --queue-url "$QUEUE_URL" \
    --attributes "file://$ATTR_FILE"

rm -f "$ATTR_FILE"
}

create_policy "$SYNC_QUEUE_URL" "$SYNC_QUEUE_ARN"
create_policy "$ANALYTICS_QUEUE_URL" "$ANALYTICS_QUEUE_ARN"
create_policy "$HISTORY_QUEUE_URL" "$HISTORY_QUEUE_ARN"

###############################################
# SNS Subscriptions
###############################################

echo "Subscribing queues..."

# RawMessageDelivery=true so each queue receives the PlaybackEvent JSON directly as the
# message body -- without it, SQS wraps it in the SNS notification envelope
# ({"Type": "Notification", "Message": "<json>", ...}) and every consumer would need to
# unwrap that itself before parsing.
awslocal sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$SYNC_QUEUE_ARN" \
    --attributes '{"RawMessageDelivery":"true"}'

awslocal sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$ANALYTICS_QUEUE_ARN" \
    --attributes '{"RawMessageDelivery":"true"}'

awslocal sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$HISTORY_QUEUE_ARN" \
    --attributes '{"RawMessageDelivery":"true"}'

###############################################
# CloudFormation: CloudFront (OAC + signed URL key group)
###############################################
# NOTE: everything above this point (S3/SNS/SQS) has already succeeded
# by the time we get here, since the script runs top-to-bottom under
# `set -e`. A failure below only affects the CloudFront stack.

echo "Deploying CloudFront stack via CloudFormation..."

PUBLIC_KEY_FILE="/etc/localstack/keys/public_key.pem"
STACK_NAME="syncbeat-cloudfront"
PARAMS_FILE="/tmp/cf-params.json"

if [ ! -f "$PUBLIC_KEY_FILE" ]; then
  echo "!! $PUBLIC_KEY_FILE not found in the container."
  echo "!! Run ./localstack/generate-keys.sh on the HOST before 'docker compose up', then 'docker compose down && docker compose up -d'."
  echo "!! Skipping CloudFront stack for now."
else
  # Build the parameters file with python3 (guaranteed present in the
  # localstack image) instead of passing the multiline PEM inline on
  # the CLI, which the shorthand --parameters parser can mangle.
  python3 - "$PUBLIC_KEY_FILE" "$PARAMS_FILE" <<'PYEOF'
import json, sys
key_path, out_path = sys.argv[1], sys.argv[2]
with open(key_path) as f:
    public_key_pem = f.read()
params = [
    {"ParameterKey": "BucketName", "ParameterValue": "syncbeat-audio"},
    {"ParameterKey": "PublicKeyPEM", "ParameterValue": public_key_pem},
]
with open(out_path, "w") as f:
    json.dump(params, f)
PYEOF

  set +e  # don't let a CFN failure look like a silent script crash

  if awslocal cloudformation describe-stacks --stack-name "$STACK_NAME" >/dev/null 2>&1; then
    echo "Stack $STACK_NAME already exists, skipping create."
  else
    echo "Running create-stack..."
    awslocal cloudformation create-stack \
        --stack-name "$STACK_NAME" \
        --template-body "file:///etc/localstack/cloudformation/cloudfront.yaml" \
        --parameters "file://$PARAMS_FILE"
    CREATE_EXIT=$?

    if [ $CREATE_EXIT -ne 0 ]; then
      echo "!! create-stack failed with exit code $CREATE_EXIT. See error above."
    else
      echo "Waiting for stack to finish creating..."
      awslocal cloudformation wait stack-create-complete --stack-name "$STACK_NAME"
      WAIT_EXIT=$?

      if [ $WAIT_EXIT -ne 0 ]; then
        echo "!! Stack did not reach CREATE_COMPLETE. Recent events:"
        awslocal cloudformation describe-stack-events \
            --stack-name "$STACK_NAME" \
            --query "StackEvents[?contains(ResourceStatus, 'FAILED')].[LogicalResourceId,ResourceStatusReason]" \
            --output table
      fi
    fi
  fi

  DISTRIBUTION_ID=$(awslocal cloudformation describe-stacks \
      --stack-name "$STACK_NAME" \
      --query "Stacks[0].Outputs[?OutputKey=='DistributionId'].OutputValue" \
      --output text 2>/dev/null)

  DISTRIBUTION_DOMAIN=$(awslocal cloudformation describe-stacks \
      --stack-name "$STACK_NAME" \
      --query "Stacks[0].Outputs[?OutputKey=='DistributionDomainName'].OutputValue" \
      --output text 2>/dev/null)

  KEY_GROUP_ID=$(awslocal cloudformation describe-stacks \
      --stack-name "$STACK_NAME" \
      --query "Stacks[0].Outputs[?OutputKey=='KeyGroupId'].OutputValue" \
      --output text 2>/dev/null)

  set -e
fi

echo ""
echo "========== LocalStack Ready =========="
echo ""
echo "Bucket:"
echo "  syncbeat-audio"
echo ""
echo "Topic:"
echo "  room-events-topic.fifo"
echo ""
echo "Queues:"
echo "  sync-queue.fifo"
echo "  analytics-queue.fifo"
echo "  activity-log-queue.fifo"
echo ""
echo "DLQs:"
echo "  sync-queue-dlq.fifo"
echo "  analytics-queue-dlq.fifo"
echo "  activity-log-queue-dlq.fifo"
echo ""
echo "CloudFront:"
echo "  Distribution ID:     ${DISTRIBUTION_ID:-not created}"
echo "  Distribution Domain: ${DISTRIBUTION_DOMAIN:-not created}"
echo "  Key Group ID:        ${KEY_GROUP_ID:-not created}"
echo ""