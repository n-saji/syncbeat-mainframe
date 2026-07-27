package com.syncbeat.mainframe.syncbeatmainframe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthRequestDto {
	@Email
	@NotBlank
	private String email;

	@NotBlank
	@Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
	private String password;
}
