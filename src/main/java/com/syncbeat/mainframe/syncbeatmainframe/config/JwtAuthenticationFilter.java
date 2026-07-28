package com.syncbeat.mainframe.syncbeatmainframe.config;

import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import com.syncbeat.mainframe.syncbeatmainframe.repository.UserRepository;
import com.syncbeat.mainframe.syncbeatmainframe.service.JWTService;
import com.syncbeat.mainframe.syncbeatmainframe.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JWTService jwtService;
	private final UserRepository userRepository;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri.startsWith("/api/auth/")
				|| uri.equals("/api/users/create")
				|| uri.equals("/error");
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request,
	                                @NonNull HttpServletResponse response,
	                                @NonNull FilterChain filterChain)
			throws IOException, ServletException {

		if (SecurityContextHolder.getContext().getAuthentication() == null) {

			String token = extractToken(request);

			if (token != null && jwtService.validateAccessToken(token)) {
				String userId = jwtService.getSubjectFromToken(token);
				if (userId != null) {
					try {
						UUID uid = UUID.fromString(userId);
						User principal;
						try {
							principal =
									userRepository.findById(uid).orElseThrow(() -> new RuntimeException("User not found with id: " + uid));

							if (!principal.getActive()) {
								response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
								return;
							}

						} catch (Exception e) {
							response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
							return;
						}
						var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
						auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(auth);
					} catch (IllegalArgumentException ignored) {
						// malformed UUID subject — leave request unauthenticated
					}
				}
			}
		}

		filterChain.doFilter(request, response);

	}

	private String extractToken(HttpServletRequest request) {
		// check cookie first, fallback to Authorization header
		if (request.getCookies() != null) {
			for (Cookie c : request.getCookies()) {
				if ("access_token".equals(c.getName())) return c.getValue();
			}
		}
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}
}
