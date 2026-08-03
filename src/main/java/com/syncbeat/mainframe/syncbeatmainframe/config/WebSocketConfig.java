package com.syncbeat.mainframe.syncbeatmainframe.config;

import com.syncbeat.mainframe.syncbeatmainframe.repository.UserRepository;
import com.syncbeat.mainframe.syncbeatmainframe.service.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP endpoint registration. JWT auth happens at handshake time, via {@link JWTHandshakeInterceptor}
 * validating the {@code access_token} httpOnly cookie on the HTTP Upgrade request, with
 * {@link UserHandshakeHandler} promoting the resolved user into the STOMP session principal. The STOMP
 * CONNECT frame itself needs no Authorization header — {@link WebSocketSecurityConfig} just checks the
 * principal is already set.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final JWTService jwtService;
	private final UserRepository userRepository;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
				.addInterceptors(new JWTHandshakeInterceptor(jwtService, userRepository))
				.setHandshakeHandler(new UserHandshakeHandler())
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
