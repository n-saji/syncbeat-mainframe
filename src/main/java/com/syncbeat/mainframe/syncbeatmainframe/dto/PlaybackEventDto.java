package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Wire format published to SNS's room-events-topic. Field names must match
 * syncbeat_sync.models.PlaybackEvent exactly (pydantic, snake_case, no aliasing).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackEventDto {
	@JsonProperty("room_id")
	private String roomId;

	@JsonProperty("event_type")
	private PlaybackEventType eventType;

	@JsonProperty("timestamp")
	private long timestamp;

	@JsonProperty("position_ms")
	private Long positionMs;

	@JsonProperty("track_id")
	private String trackId;

	@JsonProperty("user_id")
	private String userId;

	@JsonProperty("extra")
	@Builder.Default
	private Map<String, Object> extra = Map.of();
}
