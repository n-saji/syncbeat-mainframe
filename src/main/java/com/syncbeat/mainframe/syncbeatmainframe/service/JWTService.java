package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.TokenResponseDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;

@Service
public class JWTService {
	private SecretKey secretKey;
	private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

	public JWTService(@Value("${jwt.secret}") String secretKey) {
		this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
	}

	public TokenResponseDto generateToken(@NotNull String userId) {

		HashMap<String, String> accessClaims = new HashMap<>();
		accessClaims.put("type", "access_token");
		String accessToken = Jwts.builder().claims(accessClaims)
				.subject(userId)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 minutes
				.signWith(secretKey)
				.compact();



		HashMap<String, String> refreshClaims = new HashMap<>();
		refreshClaims.put("type", "refresh_token");
		String refreshToken = Jwts.builder().claims(refreshClaims)
				.subject(userId)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 24 hours
				.signWith(secretKey)
				.compact();

		return new TokenResponseDto(accessToken, refreshToken);
	}

	public boolean validateAccessToken(@NotNull String accessToken){
		try{
					Claims c = Jwts.parser()
							.verifyWith(secretKey)
							.build()
							.parseSignedClaims(accessToken).getPayload();
			return "access_token".equals(c.get("type"));
		}catch (Exception e){
			logger.warn("Access token validation failed: {}", e.getMessage(), e);
			return false;
		}
	}

	public boolean validateRefreshToken(@NotNull String refreshToken){
		try{
			Claims c = Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(refreshToken).getPayload();
			return "refresh_token".equals(c.get("type"));
		}catch (Exception e){
			logger.warn("Refresh token validation failed: {}", e.getMessage(), e);
			return false;
		}
	}

	/** Expecting only access token **/
	public String getSubjectFromToken(@NotNull String accessToken){
		Claims claims = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(accessToken)
				.getPayload();
		if (claims.get("type").equals("access_token") || claims.get("type").equals("refresh_token")) {
			return claims.get("subject").toString();
		}
		return null;
	}

}
