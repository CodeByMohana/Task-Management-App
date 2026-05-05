package com.app.taskmanagement.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;
	private final long accessTokenExpiryMs;
	private final long refreshTokenExpiryMs;

	public JwtTokenProvider(@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
			@Value("${jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
		this.accessTokenExpiryMs = accessTokenExpiryMs;
		this.refreshTokenExpiryMs = refreshTokenExpiryMs;
	}

	public String generateAccessToken(int userId, String email, String role) {
		return Jwts.builder().subject(String.valueOf(userId)).claim("email", email).claim("role", role)
				.claim("type", "access").issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs)).signWith(secretKey).compact();
	}

	public String generateRefreshToken(int userId) {
		return Jwts.builder().subject(String.valueOf(userId)).claim("type", "refresh").issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + refreshTokenExpiryMs)).signWith(secretKey).compact();
	}

	public Claims parseToken(String token) {
		return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
	}

	public int extractUserId(String token) {
		return Integer.parseInt(parseToken(token).getSubject());
	}

	public boolean validateToken(String token) {
		try {
			parseToken(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public long getRefreshTokenExpiryMs() {
		return refreshTokenExpiryMs;
	}
}