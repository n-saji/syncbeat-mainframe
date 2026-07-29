package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.AuthRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.AuthResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TokenResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final UserService userService;
	private final JWTService jwtService;


	public AuthResponseDto loginUser(AuthRequestDto authRequestDto) {
		// Single credential check (no separate "does this email exist" check) so a
		// failed login never reveals whether the email is registered.
		userService.verifyUserCredentials(authRequestDto);
		UserResponseDto userResponseDto = userService.getUserByEmail(authRequestDto.getEmail());
		return AuthResponseDto.builder()
				.user(userResponseDto)
				.build();

	}

	public TokenResponseDto refreshTokens(String refreshToken) {
		if (refreshToken == null || !jwtService.validateRefreshToken(refreshToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
		}

		String userId = jwtService.getSubjectFromRefreshToken(refreshToken);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
		}


		try {
			userService.getUserById(UUID.fromString(userId));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
		} catch (ResponseStatusException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists");
		}

		return jwtService.generateToken(userId);
	}
}
