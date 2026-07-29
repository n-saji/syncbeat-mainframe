CREATE TABLE IF NOT EXISTS users
(
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    f_name     VARCHAR(255)        NOT NULL,
    l_name     VARCHAR(255)        NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255)        NOT NULL,
    is_admin   BOOLEAN          DEFAULT FALSE,
    is_active  BOOLEAN          DEFAULT TRUE,
    created_at timestamp        DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tracks
(
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255)        NOT NULL,
    artist      VARCHAR(255)        NOT NULL,
    s3_key      VARCHAR(255),
    duration_ms   INTEGER             NOT NULL,
    created_at timestamp        DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS playlists
(
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255)        NOT NULL,
    user_id    uuid REFERENCES users(id) ON DELETE CASCADE,
    created_at timestamp        DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS playlist_tracks
(
    playlist_id uuid REFERENCES playlists(id) ON DELETE CASCADE,
    track_id    uuid REFERENCES tracks(id) ON DELETE CASCADE,
    PRIMARY KEY (playlist_id, track_id)
);

CREATE TABLE IF NOT EXISTS rooms
(
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    created_by uuid REFERENCES users (id) ON DELETE CASCADE,
    is_public  BOOLEAN          DEFAULT FALSE,
    is_active  BOOLEAN          DEFAULT TRUE,
    created_at timestamp        DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (name, created_by)
);

CREATE TABLE IF NOT EXISTS room_history
(
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id    uuid REFERENCES rooms(id) ON DELETE CASCADE,
    track_id   uuid REFERENCES tracks(id) ON DELETE CASCADE,
    played_by    uuid REFERENCES users(id) ON DELETE CASCADE,
    played_at timestamp        DEFAULT CURRENT_TIMESTAMP,
    action     VARCHAR(255) NOT NULL,
    created_at timestamp        DEFAULT CURRENT_TIMESTAMP
);
