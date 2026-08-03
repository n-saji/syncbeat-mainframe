package com.syncbeat.mainframe.syncbeatmainframe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * The STOMP session principal is already set at handshake time by {@link UserHandshakeHandler}
 * (from the {@code access_token} cookie, via {@link JWTHandshakeInterceptor}), so this just
 * refuses to let an unauthenticated handshake's CONNECT frame through — defense in depth, since
 * {@link JWTHandshakeInterceptor} should already have rejected the handshake itself.
 */
@Configuration
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor =
						MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

				if (StompCommand.CONNECT.equals(accessor.getCommand()) && accessor.getUser() == null) {
					throw new AuthenticationException("Unauthenticated WS handshake") {};
				}
				return message;
			}
		});
	}
}
