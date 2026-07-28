package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRequestDto {
	private String name;
	@JsonProperty("is_public")
	private boolean isPublic;
	@JsonProperty("is_active")
	private boolean active = true;
	// created by would be explicitly found from jwt
}
