package com.syncbeat.mainframe.syncbeatmainframe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {
	private String firstName;
	private String lastName;
	private String email;
	private String password;
	private Boolean admin;
	private Boolean active;
}
