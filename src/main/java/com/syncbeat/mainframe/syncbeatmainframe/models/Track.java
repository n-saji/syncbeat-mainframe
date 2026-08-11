package com.syncbeat.mainframe.syncbeatmainframe.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tracks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {
	@Id
	@UuidGenerator
	private UUID id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String artist;

	@Column(name = "s3_key")
	private String s3Key;

	@Column(name = "duration_ms", nullable = false)
	private Integer durationMs;

	// Pending increments live in Redis (`analytics:trending:{trackId}`, incremented by
	// syncbeat-analytics on new-song-play events) and get flushed in here periodically -
	// this column is the durable, source-of-truth total, not a live-updating counter.
	// columnDefinition matters here, not just cosmetic: without it, Hibernate's
	// ddl-auto=update generates a bare "add column play_count bigint not null" with no
	// DEFAULT, which Postgres rejects outright on a table that already has rows (this is
	// also what Flyway V3 does when FLYWAY_ENABLED=true, so ddl-auto=update and Flyway
	// now produce equivalent DDL either way).
	@Column(name = "play_count", nullable = false, columnDefinition = "bigint not null default 0")
	@Builder.Default
	private Long playCount = 0L;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		updatedAt = LocalDateTime.now();
		createdAt = LocalDateTime.now();
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = LocalDateTime.now();
	}
}

