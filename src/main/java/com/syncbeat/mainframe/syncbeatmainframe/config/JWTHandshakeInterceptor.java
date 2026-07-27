package com.syncbeat.mainframe.syncbeatmainframe.config;
import com.syncbeat.mainframe.syncbeatmainframe.service.JWTService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;



public class JWTHandshakeInterceptor implements HandshakeInterceptor {


	@Override
	public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
	                               @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) throws Exception {

//		String token = extractToken(request);
//		if (token == null) {
//			return false;
//		}
//		attributes.put("token", token);

//		token coming from headers and proccessed in stage 2
//		 use this for rate limiting
		return true;
	}

	@Override
	public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
	                           @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {

	}

	private String extractToken(ServerHttpRequest request) {
		// check cookie first, fallback to Authorization header
		String header = request.getHeaders().getFirst("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}

}
