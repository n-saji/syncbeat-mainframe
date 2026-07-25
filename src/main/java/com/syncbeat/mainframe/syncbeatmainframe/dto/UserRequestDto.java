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
	private String f_name;
	private String l_name;
	private String email;
	private String password;
	private Boolean is_admin;
	private Boolean is_active;
}
