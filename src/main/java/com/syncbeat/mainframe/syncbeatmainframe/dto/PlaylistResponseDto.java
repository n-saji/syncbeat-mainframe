package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.syncbeat.mainframe.syncbeatmainframe.models.Playlist;
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
public class PlaylistResponseDto {
	private UUID id;
	private String name;
	@JsonProperty("user_id")
	private UUID userId;
	@JsonProperty("created_at")
	private LocalDateTime createdAt;
	@JsonProperty("updated_at")
	private LocalDateTime updatedAt;

	public static PlaylistResponseDto fromEntity(Playlist playlist) {
		if (playlist == null) return null;
		return PlaylistResponseDto.builder()
				.id(playlist.getId())
				.name(playlist.getName())
				.userId(playlist.getUserId())
				.createdAt(playlist.getCreatedAt())
				.updatedAt(playlist.getUpdatedAt())
				.build();
	}
}

