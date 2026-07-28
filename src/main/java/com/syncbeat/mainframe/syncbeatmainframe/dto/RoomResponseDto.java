package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.syncbeat.mainframe.syncbeatmainframe.models.Room;
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
public class RoomResponseDto {
	private UUID id;
	private String name;
	private UserResponseDto user;
	@JsonProperty("is_public")
	private boolean isPublic;
	@JsonProperty("is_active")
	private boolean active;
	@JsonProperty("created_at")
	private LocalDateTime createdAt;
	@JsonProperty("updated_at")
	private LocalDateTime updatedAt;

	public static RoomResponseDto fromEntity(Room room) {
		if (room == null) return null;
		return RoomResponseDto.builder()
				.id(room.getId())
				.name(room.getName())
				.user(UserResponseDto.fromEntity(room.getCreatedBy()))
				.isPublic(room.isPublic())
				.active(room.isActive())
				.createdAt(room.getCreatedAt())
				.updatedAt(room.getUpdatedAt())
				.build();
	}
}
