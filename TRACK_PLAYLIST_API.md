# Track and Playlist Management API

This document describes the Track and Playlist management system for SyncBeat, including S3 and CloudFront integration for secure music streaming.

## Overview

The system implements a complete music track management and playlist creation workflow:

1. **Admin uploads track metadata** → Receive track ID
2. **Admin gets presigned S3 URL** → Upload audio file directly to S3
3. **Admin confirms S3 key** → Update track with S3 location
4. **Users create playlists** → Add track IDs to their playlists
5. **Users stream tracks** → Request signed CloudFront URL for playback

## Database Schema

### Tables Created

All tables are defined in `V1__create_base_tables.sql`:

- **tracks** - Music track catalog
  - `id` (UUID, PK)
  - `title` (VARCHAR)
  - `artist` (VARCHAR)
  - `s3_key` (VARCHAR) - Path in S3: `tracks/{track_id}/original.mp3`
  - `duration_ms` (INTEGER)
  - `created_at`, `updated_at` (TIMESTAMP)

- **playlists** - User playlists
  - `id` (UUID, PK)
  - `name` (VARCHAR)
  - `user_id` (UUID, FK)
  - `created_at`, `updated_at` (TIMESTAMP)

- **playlist_tracks** - Playlist track associations (many-to-many)
  - `playlist_id` (UUID, FK, PK)
  - `track_id` (UUID, FK, PK)

## API Endpoints

### Track Management

#### Create Track (Admin Only)
```
POST /api/v1/tracks
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "title": "Song Name",
  "artist": "Artist Name",
  "duration_ms": 240000
}

Response: 201 Created
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Song Name",
  "artist": "Artist Name",
  "duration_ms": 240000,
  "s3_key": null,
  "created_at": "2024-07-29T10:30:00",
  "updated_at": "2024-07-29T10:30:00"
}
```

#### Get Presigned Upload URL (Admin Only)
```
GET /api/v1/tracks/{track_id}/presigned-url
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "track_id": "550e8400-e29b-41d4-a716-446655440000",
  "presigned_url": "https://syncbeat-tracks.s3.us-east-1.amazonaws.com/tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3?...",
  "upload_path": "tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3"
}
```

**Usage Flow:**
1. Client receives presigned URL
2. Client performs PUT request to the presigned URL with audio file:
   ```bash
   curl -X PUT -H "Content-Type: audio/mpeg" \
     --data-binary @song.mp3 \
     "https://syncbeat-tracks.s3.us-east-1.amazonaws.com/tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3?..."
   ```

#### Update Track S3 Key (Admin Only)
```
PATCH /api/v1/tracks/{track_id}/s3-key
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "s3_key": "tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3"
}

Response: 200 OK
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Song Name",
  "artist": "Artist Name",
  "duration_ms": 240000,
  "s3_key": "tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3",
  "created_at": "2024-07-29T10:30:00",
  "updated_at": "2024-07-29T10:35:00"
}
```

#### List All Tracks (Public Catalog)
```
GET /api/v1/tracks
Authorization: Bearer {jwt_token}

Response: 200 OK
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Song Name",
    "artist": "Artist Name",
    "duration_ms": 240000,
    "s3_key": "tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3",
    "created_at": "2024-07-29T10:30:00",
    "updated_at": "2024-07-29T10:35:00"
  },
  ...
]
```

#### Get Track by ID
```
GET /api/v1/tracks/{track_id}
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Song Name",
  "artist": "Artist Name",
  "duration_ms": 240000,
  "s3_key": "tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3",
  "created_at": "2024-07-29T10:30:00",
  "updated_at": "2024-07-29T10:35:00"
}
```

#### Get Signed CloudFront Streaming URL
```
GET /api/v1/tracks/{track_id}/stream-url
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "track_id": "550e8400-e29b-41d4-a716-446655440000",
  "stream_url": "https://d111111abcdef8.cloudfront.net/tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3?Policy=...",
  "expires_at": 1722273000
}
```

**Usage:**
- Use the `stream_url` directly in an HTML5 `<audio>` tag:
  ```html
  <audio controls>
    <source src="{stream_url}" type="audio/mpeg">
  </audio>
  ```
- The URL is valid for ~1 hour by default (configurable)
- Request a new URL when it expires

#### Delete Track (Admin Only)
```
DELETE /api/v1/tracks/{track_id}
Authorization: Bearer {jwt_token}

Response: 204 No Content
```

### Playlist Management

#### Create Playlist
```
POST /api/v1/playlists
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "My Favorite Songs",
  "track_ids": [
    "550e8400-e29b-41d4-a716-446655440000",
    "660e8400-e29b-41d4-a716-446655440001"
  ]
}

Response: 201 Created
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "name": "My Favorite Songs",
  "user_id": "880e8400-e29b-41d4-a716-446655440003",
  "created_at": "2024-07-29T10:40:00",
  "updated_at": "2024-07-29T10:40:00"
}
```

#### List User's Playlists
```
GET /api/v1/playlists
Authorization: Bearer {jwt_token}

Response: 200 OK
[
  {
    "id": "770e8400-e29b-41d4-a716-446655440002",
    "name": "My Favorite Songs",
    "user_id": "880e8400-e29b-41d4-a716-446655440003",
    "created_at": "2024-07-29T10:40:00",
    "updated_at": "2024-07-29T10:40:00"
  },
  ...
]
```

#### Get Playlist with Tracks
```
GET /api/v1/playlists/{playlist_id}
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "name": "My Favorite Songs",
  "user_id": "880e8400-e29b-41d4-a716-446655440003",
  "tracks": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Song Name",
      "artist": "Artist Name",
      "duration_ms": 240000,
      "s3_key": "tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3",
      "created_at": "2024-07-29T10:30:00",
      "updated_at": "2024-07-29T10:35:00"
    },
    ...
  ],
  "created_at": "2024-07-29T10:40:00",
  "updated_at": "2024-07-29T10:40:00"
}
```

#### Add Tracks to Playlist
```
POST /api/v1/playlists/{playlist_id}/tracks
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "track_ids": [
    "550e8400-e29b-41d4-a716-446655440000",
    "990e8400-e29b-41d4-a716-446655440004"
  ]
}

Response: 204 No Content
```

#### Remove Track from Playlist
```
DELETE /api/v1/playlists/{playlist_id}/tracks/{track_id}
Authorization: Bearer {jwt_token}

Response: 204 No Content
```

#### Update Playlist Name
```
PUT /api/v1/playlists/{playlist_id}
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "New Playlist Name"
}

Response: 200 OK
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "name": "New Playlist Name",
  "user_id": "880e8400-e29b-41d4-a716-446655440003",
  "created_at": "2024-07-29T10:40:00",
  "updated_at": "2024-07-29T10:45:00"
}
```

#### Delete Playlist
```
DELETE /api/v1/playlists/{playlist_id}
Authorization: Bearer {jwt_token}

Response: 204 No Content
```

## Configuration

### Environment Variables

Add these to your `.env` file or Docker Compose configuration:

```bash
# S3 Configuration
AWS_S3_BUCKET_NAME=syncbeat-tracks
AWS_S3_PRESIGNED_EXPIRATION=900  # 15 minutes in seconds

# CloudFront Configuration
CLOUDFRONT_DOMAIN_NAME=d111111abcdef8.cloudfront.net
CLOUDFRONT_KEY_PAIR_ID=APKAJIXPF5JZ7EXAMPLE
CLOUDFRONT_PRIVATE_KEY_PATH=/app/keys/private_key.pem
CLOUDFRONT_EXPIRATION_SECONDS=3600  # 1 hour

# AWS General
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key
AWS_REGION=us-east-1
AWS_ENDPOINT=http://localhost:4566  # For LocalStack
```

### LocalStack Setup

For local development with LocalStack:

1. **Create S3 Bucket:**
   ```bash
   awslocal s3 mb s3://syncbeat-tracks
   ```

2. **Create CloudFront Distribution** (optional, for testing):
   - Use LocalStack's CloudFront simulation or skip for development
   - For production, use AWS CloudFormation with the provided template

3. **Generate CloudFront Key Pair:**
   - Run the provided script: `./localstack/generate-keys.sh`
   - Keys are stored in `./localstack/keys/`

## Architecture Notes

### Upload Flow
1. Admin creates track metadata (title, artist, duration)
2. Admin requests presigned URL
3. Client uploads file directly to S3 using presigned URL (bypasses backend)
4. Admin confirms track with S3 key

### Streaming Flow
1. User requests signed CloudFront URL
2. Backend generates RSA-SHA1 signed policy
3. Client makes HTTP request to CloudFront URL
4. CloudFront validates signature and streams content from S3
5. URL expires after configured time (default 1 hour)

### Security
- Presigned URLs have limited lifetime (15 minutes by default)
- CloudFront signed URLs use RSA-SHA1 ciphers
- S3 bucket is not directly accessible (only via CloudFront)
- Only authenticated users can access tracks
- Users can only modify their own playlists

## S3 Key Naming Convention

All tracks follow this S3 key structure:
```
tracks/{track_uuid}/original.mp3
```

Example:
```
tracks/550e8400-e29b-41d4-a716-446655440000/original.mp3
```

This allows for future file versions or formats:
- `tracks/{track_uuid}/original.mp3` - Original upload
- `tracks/{track_uuid}/compressed.mp3` - Compressed version
- `tracks/{track_uuid}/metadata.json` - Track metadata

## Error Handling

Common HTTP Status Codes:

- **201 Created** - Resource created successfully
- **204 No Content** - Operation successful, no content returned
- **400 Bad Request** - Invalid input (validation failed)
- **401 Unauthorized** - Missing or invalid JWT token
- **403 Forbidden** - User doesn't have permission (e.g., accessing another user's playlist)
- **404 Not Found** - Resource not found
- **500 Internal Server Error** - Server error

Error Response Format:
```json
{
  "error": "Error message",
  "timestamp": "2024-07-29T10:50:00",
  "path": "/api/v1/tracks/invalid-uuid"
}
```

## Testing

### Test Track Upload Workflow
```bash
# 1. Create track
curl -X POST http://localhost:8080/api/v1/tracks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Song","artist":"Test Artist","duration_ms":240000}'

# 2. Get presigned URL
curl http://localhost:8080/api/v1/tracks/$TRACK_ID/presigned-url \
  -H "Authorization: Bearer $TOKEN"

# 3. Upload file to S3
curl -X PUT -H "Content-Type: audio/mpeg" \
  --data-binary @song.mp3 \
  "$PRESIGNED_URL"

# 4. Update track with S3 key
curl -X PATCH http://localhost:8080/api/v1/tracks/$TRACK_ID/s3-key \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"s3_key":"tracks/'$TRACK_ID'/original.mp3"}'

# 5. Get stream URL
curl http://localhost:8080/api/v1/tracks/$TRACK_ID/stream-url \
  -H "Authorization: Bearer $TOKEN"
```

### Test Playlist Operations
```bash
# Create playlist
curl -X POST http://localhost:8080/api/v1/playlists \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Playlist","track_ids":["'$TRACK_ID_1'","'$TRACK_ID_2'"]}'

# Get playlist with tracks
curl http://localhost:8080/api/v1/playlists/$PLAYLIST_ID \
  -H "Authorization: Bearer $TOKEN"

# Add track to playlist
curl -X POST http://localhost:8080/api/v1/playlists/$PLAYLIST_ID/tracks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"track_ids":["'$TRACK_ID_3'"]}'
```

## Performance Considerations

- Presigned URLs are generated on-demand (minimal caching)
- CloudFront signed URLs are cached in browser (1 hour default)
- Database queries are optimized with proper indexes
- S3 bucket allows parallel uploads from multiple clients

## Future Enhancements

1. **Batch Operations** - Upload multiple tracks at once
2. **Track Search** - Full-text search on title/artist
3. **Public Playlists** - Share playlists with other users
4. **Playlist Collaboration** - Multiple users can edit playlists
5. **Track Analytics** - Track play counts and user preferences
6. **Transcoding** - Auto-convert uploaded files to standard formats

