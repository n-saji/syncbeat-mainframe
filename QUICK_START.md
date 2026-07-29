# Quick Start Guide - Track & Playlist Management

## 5-Minute Setup

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL running
- AWS account (or LocalStack for development)

### Build Project

```bash
cd /Users/nikhilsaji/Desktop/Practise/SyncBeat/syncbeat-mainframe
./mvnw clean package -DskipTests
```

### Configure Environment

Create `.env` file or set environment variables:

```bash
# Database (already configured)
DB_URL=jdbc:postgresql://localhost:5432/syncbeat
DB_USER=postgres
DB_PASSWORD=postgres

# AWS (LocalStack for dev)
AWS_REGION=us-east-1
AWS_ACCESS_KEY=test
AWS_SECRET_KEY=test
AWS_ENDPOINT=http://localhost:4566
AWS_S3_BUCKET_NAME=syncbeat-audio

# CloudFront (set after creating distribution)
CLOUDFRONT_DOMAIN_NAME=d111111abcdef8.cloudfront.net
CLOUDFRONT_KEY_PAIR_ID=APKAJIXPF5JZ7EXAMPLE
CLOUDFRONT_PRIVATE_KEY_PATH=/app/keys/private_key.pem
CLOUDFRONT_EXPIRATION_SECONDS=3600
```

### Run Application

```bash
# Using Maven
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package -DskipTests
java -jar target/syncbeat-mainframe-0.0.1-SNAPSHOT.jar
```

Server runs on `http://localhost:8080`

## Testing the API

### 1. Get Authentication Token

```bash
# Login first (assuming AuthController has login endpoint)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password"}'

# Extract token from response
export TOKEN="your_jwt_token_here"
```

### 2. Create a Track

```bash
curl -X POST http://localhost:8080/api/v1/tracks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"My Song",
    "artist":"My Artist",
    "duration_ms":240000
  }'

# Response includes track_id
export TRACK_ID="550e8400-e29b-41d4-a716-446655440000"
```

### 3. Get Presigned Upload URL

```bash
curl http://localhost:8080/api/v1/tracks/$TRACK_ID/presigned-url \
  -H "Authorization: Bearer $TOKEN"

# Response includes presigned_url
export PRESIGNED_URL="https://syncbeat-tracks.s3.us-east-1.amazonaws.com/..."
```

### 4. Upload Audio File

```bash
# Create a dummy audio file for testing
dd if=/dev/urandom of=test.mp3 bs=1024 count=100

# Upload to S3
curl -X PUT -H "Content-Type: audio/mpeg" \
  --data-binary @test.mp3 \
  "$PRESIGNED_URL"
```

### 5. Update Track S3 Key

```bash
curl -X PATCH http://localhost:8080/api/v1/tracks/$TRACK_ID/s3-key \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "s3_key":"tracks/'$TRACK_ID'/original.mp3"
  }'
```

### 6. Get Signed Streaming URL

```bash
curl http://localhost:8080/api/v1/tracks/$TRACK_ID/stream-url \
  -H "Authorization: Bearer $TOKEN"

# Response includes stream_url - use in <audio> tag
export STREAM_URL="https://d111111abcdef8.cloudfront.net/tracks/..."
```

### 7. Create a Playlist

```bash
curl -X POST http://localhost:8080/api/v1/playlists \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"My Playlist",
    "track_ids":["'$TRACK_ID'"]
  }'

# Response includes playlist_id
export PLAYLIST_ID="770e8400-e29b-41d4-a716-446655440002"
```

### 8. Get Playlist with Tracks

```bash
curl http://localhost:8080/api/v1/playlists/$PLAYLIST_ID \
  -H "Authorization: Bearer $TOKEN"
```

## Key Files

### Entity Models
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/models/Track.java`
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/models/Playlist.java`
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/models/PlaylistTrack.java`

### Services
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/service/TrackService.java`
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/service/PlaylistService.java`
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/service/S3Service.java`

### Controllers
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/controller/TrackController.java`
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/controller/PlaylistController.java`

### DTOs
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/dto/TrackRequestDto.java`
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/dto/TrackResponseDto.java`
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/dto/PlaylistRequestDto.java`
- `src/main/java/com/syncbeat/mainframe/syncbeatmainframe/dto/PlaylistResponseDto.java`
- And more in the `dto/` directory

## API Endpoints at a Glance

```
TRACKS
------
POST   /api/v1/tracks                      Create track (admin)
GET    /api/v1/tracks                      List all tracks
GET    /api/v1/tracks/{id}                 Get track by ID
GET    /api/v1/tracks/{id}/presigned-url   Get S3 upload URL (admin)
PATCH  /api/v1/tracks/{id}/s3-key          Update S3 key (admin)
GET    /api/v1/tracks/{id}/stream-url      Get streaming URL
DELETE /api/v1/tracks/{id}                 Delete track (admin)

PLAYLISTS
---------
POST   /api/v1/playlists                   Create playlist
GET    /api/v1/playlists                   List user playlists
GET    /api/v1/playlists/{id}              Get playlist with tracks
POST   /api/v1/playlists/{id}/tracks       Add tracks
DELETE /api/v1/playlists/{id}/tracks/{tid} Remove track
PUT    /api/v1/playlists/{id}              Update name
DELETE /api/v1/playlists/{id}              Delete playlist
```

## LocalStack Development

### Start LocalStack

```bash
docker-compose up -d localstack
```

### Initialize S3

```bash
awslocal s3 mb s3://syncbeat-tracks
awslocal s3 ls
```

### Generate Keys

```bash
cd localstack
./generate-keys.sh
# Creates: keys/private_key.pem and keys/public_key.pem
```

## Troubleshooting

### Port Already in Use
```bash
lsof -i :8080
kill -9 <PID>
```

### Database Connection Error
```bash
# Check PostgreSQL is running
psql -U postgres -d syncbeat -c "SELECT 1"

# Or recreate database
createdb syncbeat
psql -U postgres -d syncbeat < schema.sql
```

### AWS Credentials Not Found
```bash
# Set environment variables
export AWS_ACCESS_KEY=test
export AWS_SECRET_KEY=test
export AWS_REGION=us-east-1
export AWS_ENDPOINT=http://localhost:4566
```

### CloudFront Configuration Missing
```bash
# Verify environment variables
echo $CLOUDFRONT_DOMAIN_NAME
echo $CLOUDFRONT_KEY_PAIR_ID
echo $CLOUDFRONT_PRIVATE_KEY_PATH

# Test CloudFront utilities
# The service will throw exception if not configured
# This is expected until AWS setup is complete
```

## Documentation Files

- **IMPLEMENTATION_SUMMARY.md** - Complete technical overview
- **TRACK_PLAYLIST_API.md** - Detailed API documentation
- **S3_CLOUDFRONT_SETUP.md** - AWS infrastructure setup guide

## Common Tasks

### Seed Test Data

```bash
# Create multiple tracks
for i in {1..3}; do
  curl -X POST http://localhost:8080/api/v1/tracks \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"title\":\"Test Song $i\",\"artist\":\"Test Artist\",\"duration_ms\":240000}"
done
```

### Export API to Postman

Use the endpoints listed above to create a Postman collection:
1. Create new collection
2. Add requests for each endpoint
3. Set variables for `$TOKEN`, `$TRACK_ID`, `$PLAYLIST_ID`
4. Save and share

### Monitor Logs

```bash
# View application logs
tail -f target/logs/application.log

# Or if using Docker
docker logs -f syncbeat-api
```

## Security Notes

### For Development
- Private key stored in local file: `localstack/keys/private_key.pem`
- AWS credentials in environment: set to `test`/`test`
- HTTPS not enforced in development

### For Production
- Store private key in AWS Secrets Manager
- Use IAM roles instead of hardcoded credentials
- Enable HTTPS/TLS
- Enable CORS restrictions
- Set rate limiting
- Enable CloudTrail logging
- Rotate keys regularly

## Performance Tips

1. **Batch Uploads** - Upload multiple files in parallel
2. **URL Caching** - Cache streaming URLs in client (1 hour TTL)
3. **Lazy Loading** - Playlists load tracks on-demand
4. **Database Indexing** - Indexes on user_id, track_id
5. **CDN Caching** - CloudFront caches audio for 1-7 days

## Next Steps

1. Read **TRACK_PLAYLIST_API.md** for detailed endpoint documentation
2. Read **S3_CLOUDFRONT_SETUP.md** to set up production AWS infrastructure
3. Run test commands above
4. Create test playlists
5. Integrate with frontend
6. Set up monitoring and alarms

