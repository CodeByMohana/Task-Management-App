package com.app.taskmanagement.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * JWT token validator for API Gateway.
 *
 * This is a copy of the JwtTokenProvider from other services, but used ONLY for
 * validation — gateway never generates tokens.
 *
 * Must use the EXACT same secret as auth-service to validate the same tokens.
 */
@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;

	public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
	}

	/**
	 * Parse and validate the JWT token. Throws JwtException if token is expired,
	 * tampered, or malformed.
	 */
	public Claims parseToken(String token) {
		return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
	}

	/**
	 * Extract userId from the token's subject field.
	 */
	public int extractUserId(String token) {
		return Integer.parseInt(parseToken(token).getSubject());
	}

	/**
	 * Extract the user's role from token claims.
	 */
	public String extractRole(String token) {
		return (String) parseToken(token).get("role");
	}

	/**
	 * Validate token — returns true if valid, false otherwise. Never throws —
	 * returns false on any error.
	 */
	public boolean validateToken(String token) {
		try {
			Claims claims = parseToken(token);
			// Reject refresh tokens used as access tokens
			return !"refresh".equals(claims.get("type"));
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}
}