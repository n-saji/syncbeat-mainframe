# S3 and CloudFront Setup Guide

This guide covers setting up AWS S3 and CloudFront for the SyncBeat music streaming service.

## Overview

The architecture uses:
- **Amazon S3** - Storage for audio files
- **Amazon CloudFront** - CDN for fast, secure streaming
- **Origin Access Control (OAC)** - Prevents direct S3 access, forces CloudFront routing
- **Signed URLs** - Temporary access tokens for streaming

## Step 1: Create S3 Bucket

### Using AWS Console

1. Navigate to S3 service
2. Click "Create bucket"
3. **Bucket name:** `syncbeat-tracks`
4. **Region:** Choose your preferred region (e.g., `us-east-1`)
5. **Block Public Access:** Enable all blocking options
6. Click "Create bucket"

### Using AWS CLI

```bash
aws s3 mb s3://syncbeat-tracks --region us-east-1
aws s3api put-bucket-versioning \
  --bucket syncbeat-tracks \
  --versioning-configuration Status=Enabled
```

### Using LocalStack (Development)

```bash
# If using LocalStack
awslocal s3 mb s3://syncbeat-tracks
```

## Step 2: Create CloudFront Distribution

### Using AWS Console

1. Navigate to CloudFront service
2. Click "Create distribution"
3. **Origin settings:**
   - Origin domain: Select your S3 bucket
   - Origin access control setting: Click "Create control setting"
   - Name: `syncbeat-s3-oac`
   - Signing behavior: "Sign requests"
4. **Default cache behavior:**
   - Viewer protocol policy: "Redirect HTTP to HTTPS"
   - Cache policy: "CachingOptimized"
   - Origin request policy: "CORS-S3Origin"
5. **Distribution settings:**
   - Default root object: (leave empty)
   - Enable IPv6: Yes
6. Click "Create distribution"

### Update S3 Bucket Policy

After creating the distribution, CloudFront provides a policy to add to S3:

1. Go to S3 bucket
2. Click "Permissions" tab
3. Click "Bucket policy"
4. Paste the policy provided by CloudFront
5. Click "Save"

### Using Terraform/CloudFormation

See `localstack/cloudformation/cloudfront.yaml` for a sample template.

## Step 3: Create CloudFront Key Pair

### Using AWS Console

1. Navigate to CloudFront > Key pairs
2. Click "Create key pair"
3. Download the private key (save as `private_key.pem`)
4. Note the **Key pair ID**

### Using AWS CLI

```bash
aws cloudfront create-distribution-key-pair \
  --region us-east-1
```

### Using the Setup Script (LocalStack)

```bash
cd localstack
./generate-keys.sh
```

This creates:
- `keys/private_key.pem` - Private key for signing
- `keys/public_key.pem` - Public key reference

## Step 4: Create CloudFront Trusted Key Group

### Using AWS Console

1. Navigate to CloudFront > Trusted key groups
2. Click "Create key group"
3. **Name:** `syncbeat-signers`
4. Add your key pair ID
5. Click "Create key group"

### Update Distribution

1. Go to your CloudFront distribution
2. Edit the default cache behavior
3. Under "Restrict viewer access (use signed URLs/signed cookies)"
4. Select: "Use signed URLs and signed cookies"
5. Choose your trusted key group
6. Save the distribution

## Step 5: Configure Backend

### Application Properties

Update `application.properties`:

```properties
# S3 Configuration
aws.s3.bucket-name=syncbeat-tracks
aws.s3.presigned-url-expiration=900

# CloudFront Configuration
cloudfront.domain-name=d111111abcdef8.cloudfront.net
cloudfront.key-pair-id=APKAJIXPF5JZ7EXAMPLE
cloudfront.private-key-path=/app/keys/private_key.pem
cloudfront.expiration-seconds=3600

# AWS General
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key
AWS_REGION=us-east-1
AWS_ENDPOINT=http://localhost:4566  # Remove for production
```

### Docker Compose Setup

```yaml
version: '3.8'
services:
  syncbeat-api:
    image: syncbeat-mainframe:latest
    ports:
      - "8080:8080"
    environment:
      AWS_S3_BUCKET_NAME: syncbeat-tracks
      AWS_S3_PRESIGNED_EXPIRATION: 900
      CLOUDFRONT_DOMAIN_NAME: d111111abcdef8.cloudfront.net
      CLOUDFRONT_KEY_PAIR_ID: APKAJIXPF5JZ7EXAMPLE
      CLOUDFRONT_PRIVATE_KEY_PATH: /app/keys/private_key.pem
      CLOUDFRONT_EXPIRATION_SECONDS: 3600
      AWS_REGION: us-east-1
      AWS_ACCESS_KEY: your_key
      AWS_SECRET_KEY: your_secret
    volumes:
      - ./localstack/keys:/app/keys:ro
```

### Mount Private Key

Ensure the private key is accessible to the application:

```bash
# For Docker
-v $(pwd)/localstack/keys/private_key.pem:/app/keys/private_key.pem:ro

# For local development
CLOUDFRONT_PRIVATE_KEY_PATH=/full/path/to/private_key.pem
```

## Step 6: Seed Initial Tracks

### Create Seed Script

Create `localstack/init-tracks.sh`:

```bash
#!/bin/bash

API_URL="http://localhost:8080/api/v1/tracks"
AUTH_TOKEN="$1"

# Sample royalty-free tracks
TRACKS=(
  '{"title":"Background Music 1","artist":"Free Music","duration_ms":180000}'
  '{"title":"Background Music 2","artist":"Free Music","duration_ms":240000}'
  '{"title":"Ambient Sounds","artist":"Royalty Free","duration_ms":300000}'
)

for track in "${TRACKS[@]}"; do
  echo "Creating track: $track"
  
  RESPONSE=$(curl -s -X POST "$API_URL" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$track")
  
  TRACK_ID=$(echo $RESPONSE | jq -r '.id')
  echo "Created track: $TRACK_ID"
done
```

### Execute Seed Script

```bash
./localstack/init-tracks.sh $JWT_TOKEN
```

## Testing S3 Upload

### Generate Presigned URL

```bash
curl -X GET http://localhost:8080/api/v1/tracks/{track_id}/presigned-url \
  -H "Authorization: Bearer $TOKEN"
```

Response:
```json
{
  "track_id": "550e8400-e29b-41d4-a716-446655440000",
  "presigned_url": "https://syncbeat-tracks.s3.us-east-1.amazonaws.com/...",
  "upload_path": "tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3"
}
```

### Upload File to S3

```bash
PRESIGNED_URL="..."
curl -X PUT -H "Content-Type: audio/mpeg" \
  --data-binary @sample.mp3 \
  "$PRESIGNED_URL"
```

### Verify in S3

```bash
aws s3 ls s3://syncbeat-tracks/tracks/
# For LocalStack
awslocal s3 ls s3://syncbeat-tracks/tracks/
```

## Testing CloudFront Signed URLs

### Request Signed URL

```bash
curl http://localhost:8080/api/v1/tracks/{track_id}/stream-url \
  -H "Authorization: Bearer $TOKEN"
```

Response:
```json
{
  "track_id": "550e8400-e29b-41d4-a716-446655440000",
  "stream_url": "https://d111111abcdef8.cloudfront.net/tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3?Policy=...",
  "expires_at": 1722273000
}
```

### Test Streaming

```bash
# Using curl with stream URL
curl "$STREAM_URL" -o downloaded_song.mp3

# Using ffmpeg to verify
ffmpeg -i "$STREAM_URL" -f null -
```

## Troubleshooting

### Issue: "Access Denied" on CloudFront

**Cause:** Invalid signature or expired URL

**Solution:**
- Verify private key path is correct
- Check key pair ID matches CloudFront configuration
- Verify expiration time is in the future
- Check system time is synchronized (NTP)

### Issue: "SignatureDoesNotMatch"

**Cause:** Key file format or signing algorithm mismatch

**Solution:**
- Ensure private key is in PKCS#8 format
- Verify RSA key (2048-bit or larger)
- Check policy JSON format (no extra whitespace)

### Issue: Presigned URL Not Working

**Cause:** S3 bucket not accessible, expired URL, or region mismatch

**Solution:**
- Verify bucket exists and is in configured region
- Check AWS credentials
- Regenerate presigned URL (they expire)
- Verify bucket policy allows the principal

### Issue: CloudFront Distribution Not Serving Files

**Cause:** OAC not configured, origin unreachable, or caching issues

**Solution:**
- Verify OAC is attached to origin
- Check S3 bucket policy includes CloudFront principal
- Invalidate CloudFront cache
- Check origin health in CloudFront dashboard

## Security Best Practices

1. **Restrict S3 Bucket Access:**
   ```bash
   aws s3api put-bucket-public-access-block \
     --bucket syncbeat-tracks \
     --public-access-block-configuration \
     "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
   ```

2. **Enable S3 Versioning:**
   ```bash
   aws s3api put-bucket-versioning \
     --bucket syncbeat-tracks \
     --versioning-configuration Status=Enabled
   ```

3. **Enable S3 Server-Side Encryption:**
   ```bash
   aws s3api put-bucket-encryption \
     --bucket syncbeat-tracks \
     --server-side-encryption-configuration '{
       "Rules": [{
         "ApplyServerSideEncryptionByDefault": {
           "SSEAlgorithm": "AES256"
         }
       }]
     }'
   ```

4. **Rotate CloudFront Key Pairs:**
   - Create new key pair every 90 days
   - Update application configuration
   - Remove old key from trusted key group

5. **Monitor Access:**
   - Enable CloudFront access logs
   - Set up CloudWatch alarms for errors
   - Review S3 access logs regularly

## Performance Tuning

### CloudFront Caching

- **Cache-Control Headers:**
  - Set TTL to 1-7 days for immutable objects
  - Use versioning in S3 keys for cache invalidation

- **Compression:**
  - Enable gzip compression for metadata
  - S3 stores objects uncompressed (CloudFront handles compression)

### S3 Request Rate

- **Upload Throttling:** Use multipart upload for large files
- **Download Throttling:** CloudFront handles bandwidth
- **Sharding:** Consider multiple prefixes for high concurrency

### Regional Optimization

- Place S3 bucket in region closest to majority of users
- CloudFront automatically caches at edge locations
- Use Route 53 for geographic routing

## Local Development with LocalStack

### Start LocalStack

```bash
docker-compose up -d localstack
```

### Initialize S3

```bash
awslocal s3 mb s3://syncbeat-tracks
awslocal s3api put-bucket-versioning \
  --bucket syncbeat-tracks \
  --versioning-configuration Status=Enabled
```

### Generate Local Keys

```bash
cd localstack
./generate-keys.sh
```

### Configuration

Set in Docker Compose or `.env`:
```
AWS_ENDPOINT=http://localhost:4566
CLOUDFRONT_DOMAIN_NAME=localhost:4566
CLOUDFRONT_KEY_PAIR_ID=test-key
CLOUDFRONT_PRIVATE_KEY_PATH=/app/keys/private_key.pem
```

## Production Deployment Checklist

- [ ] S3 bucket created and configured
- [ ] CloudFront distribution deployed
- [ ] OAC configured and trusted key group created
- [ ] Private key stored securely (AWS Secrets Manager)
- [ ] S3 bucket logging enabled
- [ ] CloudFront logging enabled
- [ ] CloudFront caching optimized
- [ ] WAF rules applied (optional)
- [ ] Rate limiting configured
- [ ] Health checks configured
- [ ] Alarms set up for errors
- [ ] Load testing completed
- [ ] Disaster recovery plan documented

