package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.syncbeat.mainframe.syncbeatmainframe.models.User;
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
	private String firstName;
	private String lastName;
	private String email;
	private Boolean admin;
	private Boolean active;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public static UserResponseDto fromEntity(User user) {
		if (user == null) return null;
		return UserResponseDto.builder()
				.id(user.getId())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.email(user.getEmail())
				.admin(user.getAdmin())
				.active(user.getActive())
				.createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt())
				.build();
	}
}
