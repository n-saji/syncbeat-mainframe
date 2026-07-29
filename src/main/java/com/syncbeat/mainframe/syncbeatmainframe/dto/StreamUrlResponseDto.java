package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamUrlResponseDto {
	@JsonProperty("track_id")
	private UUID trackId;

	@JsonProperty("stream_url")
	private String streamUrl;

	@JsonProperty("expires_at")
	private long expiresAt;
}

