package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackRequestDto {
	@NotBlank(message = "Title is required")
	private String title;

	@NotBlank(message = "Artist is required")
	private String artist;

	@NotNull(message = "Duration is required")
	@JsonProperty("duration_ms")
	private Integer durationMs;
}

