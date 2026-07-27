package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.syncbeat.mainframe.syncbeatmainframe.models.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponseDto {
	private String message;
	private UserResponseDto user;

	public static AuthResponseDto fromEntity(UserResponseDto user, String message) {
		if (user == null) return null;
		return AuthResponseDto.builder()
				.message(message)
				.user(user).build();
	}
}
