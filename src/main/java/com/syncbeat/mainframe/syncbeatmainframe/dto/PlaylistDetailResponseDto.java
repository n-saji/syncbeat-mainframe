package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistDetailResponseDto {
	private UUID id;
	private String name;
	@JsonProperty("user_id")
	private UUID userId;
	private List<TrackResponseDto> tracks;
	@JsonProperty("created_at")
	private LocalDateTime createdAt;
	@JsonProperty("updated_at")
	private LocalDateTime updatedAt;
}

