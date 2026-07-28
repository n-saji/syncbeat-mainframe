package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.TokenResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JWTService {
	private final SecretKey secretKey;
	private static final Logger logger = LoggerFactory.getLogger(JWTService.class);
	private static final String TOKEN_TYPE = "type";
	private static final String ACCESS = "access_token";
	private static final String REFRESH = "refresh_token";

	public JWTService(@Value("${jwt.secret}") String secretKey) {
		this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
	}

	public TokenResponseDto generateToken(@NotNull String userId) {
		Map<String, Object> claims = new HashMap<>();
		claims.put(TOKEN_TYPE, ACCESS);
		String accessToken = Jwts.builder().claims(claims)
				.subject(userId)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 minutes
				.signWith(secretKey)
				.compact();


		HashMap<String, String> refreshClaims = new HashMap<>();
		refreshClaims.put(TOKEN_TYPE, REFRESH);
		String refreshToken = Jwts.builder().claims(refreshClaims)
				.subject(userId)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 24 hours
				.signWith(secretKey)
				.compact();

		return new TokenResponseDto(accessToken, refreshToken);
	}

	public boolean validateAccessToken(String token) {
		return validateToken(token, ACCESS);
	}

	public boolean validateRefreshToken(String token) {
		return validateToken(token, REFRESH);
	}

	private boolean validateToken(String token, String expectedType) {
		try {
			Claims claims = extractClaims(token);
			return expectedType.equals(claims.get(TOKEN_TYPE));
		} catch (Exception e) {
			logger.warn("Token validation failed", e);
			return false;
		}
	}

	private Claims extractClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	/** Expecting only access token **/
	public String getSubjectFromToken(@NotNull String accessToken){
		Claims claims = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(accessToken)
				.getPayload();

		if (claims.get(TOKEN_TYPE).equals(ACCESS)) {
			return claims.getSubject();
		}
		return null;
	}

}
