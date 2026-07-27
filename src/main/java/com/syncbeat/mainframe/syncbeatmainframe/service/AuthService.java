package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.AuthRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.AuthResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TokenResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final UserService userService;


	public AuthResponseDto loginUser(AuthRequestDto authRequestDto) {
		if (!userService.checkUserExistsByMail(authRequestDto.getEmail())) {
			throw new RuntimeException("User not found with email: " + authRequestDto.getEmail());
		}
		userService.verifyUserCredentials(authRequestDto);
		UserResponseDto userResponseDto = userService.getUserByEmail(authRequestDto.getEmail());
		return AuthResponseDto.builder()
				.user(userResponseDto)
				.build();

	}
}
