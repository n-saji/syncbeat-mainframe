package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.syncbeat.mainframe.syncbeatmainframe.dto.AuthRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.AuthResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TokenResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.service.AuthService;
import com.syncbeat.mainframe.syncbeatmainframe.service.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.attribute.UserPrincipal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
	private final JWTService jwtService;

	@PostMapping("/login")
	public ResponseEntity<?> login(HttpServletRequest request, @RequestBody AuthRequestDto authRequestDto) {
		// Implement your login logic here
		AuthResponseDto resp = authService.loginUser(authRequestDto);
		TokenResponseDto tokenResponseDto =
				jwtService.generateToken(authRequestDto.getEmail());

		ResponseCookie refreshToken = ResponseCookie.from("refresh_token",
				tokenResponseDto.refreshToken())
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(7 * 24 * 60 * 60) // 7 days
				.build();

		ResponseCookie accessToken = ResponseCookie.from("access_token",
				tokenResponseDto.accessToken())
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(15 * 60) // 15 minutes
				.build();



		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshToken.toString())
				.header(HttpHeaders.SET_COOKIE, accessToken.toString())
				.body(resp);
	}

}
