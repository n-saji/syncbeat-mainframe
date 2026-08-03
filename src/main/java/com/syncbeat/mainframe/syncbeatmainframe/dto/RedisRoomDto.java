package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisRoomDto {
	@JsonProperty("room_id")
	private String roomId;
	@JsonProperty("room_name")
	private String roomName;
	@JsonProperty("room_type")
	private String roomType;
	@JsonProperty("host_id")
	private String hostId;
	@JsonProperty("position_ms")
	private Long positionMs;
	@JsonProperty("is_playing")
	private Boolean isPlaying;
	@JsonProperty("members")
	private List<String> members;
	@JsonProperty("updated_at")
	private Instant updatedAt;

	public static RedisRoomDto fromRedis(String roomId,
	                                     Map<Object, Object>  hash, Set<Object> members) {
		return  RedisRoomDto.builder()
				.roomId(roomId)
				.roomName((String) hash.get("roomName"))
				.roomType((String) hash.get("roomType"))
				.hostId((String) hash.get("hostId"))
				.positionMs(Long.parseLong((String) hash.get("positionMs")))
				.isPlaying(Boolean.parseBoolean((String) hash.get("isPlaying")))
				.members(members.stream().map(m -> m != null ? m.toString() : null).toList())
				.updatedAt(Instant.parse((String) hash.get("updatedAt")))
				.build();


	}
}
