package com.syncbeat.mainframe.syncbeatmainframe.config;

import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import com.syncbeat.mainframe.syncbeatmainframe.repository.UserRepository;
import com.syncbeat.mainframe.syncbeatmainframe.service.JWTService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * Authenticates the WebSocket handshake itself (the plain HTTP Upgrade request), using the same
 * {@code access_token} httpOnly cookie REST auth uses ({@link JwtAuthenticationFilter}). The browser
 * attaches httpOnly cookies to real HTTP requests (including this one) even though JS can't read them,
 * so this is the only point in the STOMP flow where the cookie is actually usable. The resolved user is
 * stashed in the handshake attributes for {@link UserHandshakeHandler} to pick up as the STOMP principal.
 */
@RequiredArgsConstructor
public class JWTHandshakeInterceptor implements HandshakeInterceptor {

	private final JWTService jwtService;
	private final UserRepository userRepository;

	@Override
	public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
	                               @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {

		String token = extractToken(request);
		if (token == null || !jwtService.validateAccessToken(token)) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}

		String userId = jwtService.getSubjectFromToken(token);
		if (userId == null) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}

		User user;
		try {
			user = userRepository.findById(UUID.fromString(userId)).orElse(null);
		} catch (IllegalArgumentException e) {
			user = null;
		}

		if (user == null || !Boolean.TRUE.equals(user.getActive())) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}

		attributes.put("user", user);
		return true;
	}

	@Override
	public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
	                           @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {

	}

	private String extractToken(ServerHttpRequest request) {
		if (!(request instanceof ServletServerHttpRequest servletRequest)) {
			return null;
		}
		HttpServletRequest httpRequest = servletRequest.getServletRequest();

		if (httpRequest.getCookies() != null) {
			for (Cookie c : httpRequest.getCookies()) {
				if ("access_token".equals(c.getName())) return c.getValue();
			}
		}

		String header = httpRequest.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}

}
