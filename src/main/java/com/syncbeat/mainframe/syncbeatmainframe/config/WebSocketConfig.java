package com.syncbeat.mainframe.syncbeatmainframe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP endpoint registration. Actual JWT auth happens in {@link WebSocketSecurityConfig}'s
 * channel interceptor on the STOMP CONNECT frame, not at handshake time (see
 * {@link JWTHandshakeInterceptor} — deliberately permissive at handshake).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
				.addInterceptors(new JWTHandshakeInterceptor())
				.setAllowedOriginPatterns(resolveAllowedOrigins());
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	private String[] resolveAllowedOrigins() {
		String envValue = System.getenv("ALLOWED_ORIGINS");
		String origins = (envValue == null || envValue.isBlank()) ? "http://localhost:3000" : envValue;
		return origins.split(",");
	}
}
