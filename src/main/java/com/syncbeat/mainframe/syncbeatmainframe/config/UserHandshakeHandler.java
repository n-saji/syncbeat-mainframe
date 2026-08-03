package com.syncbeat.mainframe.syncbeatmainframe.config;

import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Promotes the {@link User} resolved by {@link JWTHandshakeInterceptor} (from the handshake's
 * access_token cookie) into the STOMP session's {@link Principal}, so no Authorization header is
 * needed on the STOMP CONNECT frame itself.
 */
public class UserHandshakeHandler extends DefaultHandshakeHandler {

	@Override
	@Nullable
	protected Principal determineUser(@NonNull ServerHttpRequest request, @NonNull WebSocketHandler wsHandler,
	                                   @NonNull Map<String, Object> attributes) {
		Object user = attributes.get("user");
		if (!(user instanceof User principal)) {
			return null;
		}

		List<GrantedAuthority> authorities = Boolean.TRUE.equals(principal.getAdmin())
				? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
				: List.of(new SimpleGrantedAuthority("ROLE_USER"));

		return new UsernamePasswordAuthenticationToken(principal, null, authorities);
	}
}
