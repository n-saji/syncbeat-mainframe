package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.syncbeat.mainframe.syncbeatmainframe.models.Track;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackResponseDto {
	private UUID id;
	private String title;
	private String artist;
	@JsonProperty("duration_ms")
	private Integer durationMs;
	@JsonProperty("s3_key")
	private String s3Key;
	@JsonProperty("created_at")
	private LocalDateTime createdAt;
	@JsonProperty("updated_at")
	private LocalDateTime updatedAt;

	public static TrackResponseDto fromEntity(Track track) {
		if (track == null) return null;
		return TrackResponseDto.builder()
				.id(track.getId())
				.title(track.getTitle())
				.artist(track.getArtist())
				.durationMs(track.getDurationMs())
				.s3Key(track.getS3Key())
				.createdAt(track.getCreatedAt())
				.updatedAt(track.getUpdatedAt())
				.build();
	}
}

