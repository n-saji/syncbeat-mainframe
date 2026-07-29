package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.syncbeat.mainframe.syncbeatmainframe.dto.AuthRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.AuthResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TokenResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.service.AuthService;
import com.syncbeat.mainframe.syncbeatmainframe.service.JWTService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
	private final JWTService jwtService;

	private static final int ACCESS_TOKEN_MAX_AGE = 15 * 60; // 15 minutes
	private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

	@PostMapping("/login")
	public ResponseEntity<?> login(HttpServletRequest request, @RequestBody AuthRequestDto authRequestDto) {

		AuthResponseDto resp = authService.loginUser(authRequestDto);
		TokenResponseDto tokenResponseDto =
				jwtService.generateToken(resp.getUser().getId().toString());

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, buildCookie("refresh_token", tokenResponseDto.refreshToken(), REFRESH_TOKEN_MAX_AGE).toString())
				.header(HttpHeaders.SET_COOKIE, buildCookie("access_token", tokenResponseDto.accessToken(), ACCESS_TOKEN_MAX_AGE).toString())
				.body(resp);
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(HttpServletRequest request) {
		String refreshToken = extractCookie(request, "refresh_token");
		if (refreshToken == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
		}

		TokenResponseDto tokenResponseDto = authService.refreshTokens(refreshToken);

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, buildCookie("refresh_token", tokenResponseDto.refreshToken(), REFRESH_TOKEN_MAX_AGE).toString())
				.header(HttpHeaders.SET_COOKIE, buildCookie("access_token", tokenResponseDto.accessToken(), ACCESS_TOKEN_MAX_AGE).toString())
				.build();
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, buildCookie("refresh_token", "", 0).toString())
				.header(HttpHeaders.SET_COOKIE, buildCookie("access_token", "", 0).toString())
				.build();
	}

	private ResponseCookie buildCookie(String name, String value, int maxAgeSeconds) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(maxAgeSeconds)
				.build();
	}

	private String extractCookie(HttpServletRequest request, String name) {
		if (request.getCookies() == null) return null;
		for (Cookie c : request.getCookies()) {
			if (name.equals(c.getName())) return c.getValue();
		}
		return null;
	}
}
