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

awslocal sqs set-queue-attributes \
    --queue-url "$QUEUE_URL" \
    --attributes "{
        \"Policy\":\"{
            \\\"Version\\\":\\\"2012-10-17\\\",
            \\\"Statement\\\":[
                {
                    \\\"Effect\\\":\\\"Allow\\\",
                    \\\"Principal\\\":{\\\"AWS\\\":\\\"*\\\"},
                    \\\"Action\\\":\\\"SQS:SendMessage\\\",
                    \\\"Resource\\\":\\\"$QUEUE_ARN\\\",
                    \\\"Condition\\\":{
                        \\\"ArnEquals\\\":{
                            \\\"aws:SourceArn\\\":\\\"$TOPIC_ARN\\\"
                        }
                    }
                }
            ]
        }\"
    }"
}

create_policy "$SYNC_QUEUE_URL" "$SYNC_QUEUE_ARN"
create_policy "$ANALYTICS_QUEUE_URL" "$ANALYTICS_QUEUE_ARN"
create_policy "$HISTORY_QUEUE_URL" "$HISTORY_QUEUE_ARN"

###############################################
# SNS Subscriptions
###############################################

echo "Subscribing queues..."

awslocal sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$SYNC_QUEUE_ARN"

awslocal sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$ANALYTICS_QUEUE_ARN"

awslocal sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$HISTORY_QUEUE_ARN"

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