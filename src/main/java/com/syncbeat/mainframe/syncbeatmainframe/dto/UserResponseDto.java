package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.syncbeat.mainframe.syncbeatmainframe.models.Users;
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
public class UserResponseDto {
	private UUID id;
	private String f_name;
	private String l_name;
	private String email;
	private Boolean is_admin;
	private Boolean is_active;
	private LocalDateTime created_at;
	private LocalDateTime updated_at;

	public static UserResponseDto fromEntity(Users user) {
		if (user == null) return null;
		return UserResponseDto.builder()
				.id(user.getId())
				.f_name(user.getF_name())
				.l_name(user.getL_name())
				.email(user.getEmail())
				.is_admin(user.getIs_admin())
				.is_active(user.getIs_active())
				.created_at(user.getCreated_at())
				.updated_at(user.getUpdated_at())
				.build();
	}
}
