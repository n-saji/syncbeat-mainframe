package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Body of a client SEND to /app/room/{roomId}/action, e.g. {"type": "PAUSE", "position_ms": 4200}.
 */
@Data
public class PlaybackActionRequestDto {
	private PlaybackEventType type;

	@JsonProperty("position_ms")
	private Long positionMs;

	@JsonProperty("track_id")
	private String trackId;
}
