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

