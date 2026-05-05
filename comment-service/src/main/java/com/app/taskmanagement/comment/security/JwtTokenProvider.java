package com.app.taskmanagement.comment.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * JWT validator for comment-service.
 *
 * This service NEVER generates tokens — only validates them. The secret must
 * exactly match auth-service's jwt.secret.
 *
 * Token is provided either via: - Authorization: Bearer <token> header (from
 * Postman / API clients) - accessToken cookie (from browser, set by
 * auth-service on login) - X-User-Id / X-User-Role headers injected by the API
 * gateway (gateway already validated the token before forwarding)
 */
@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;

	public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
	}

	public Claims parseToken(String token) {
		return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
	}

	public int extractUserId(String token) {
		return Integer.parseInt(parseToken(token).getSubject());
	}

	public String extractRole(String token) {
		return (String) parseToken(token).get("role");
	}

	/**
	 * Returns true only for valid access tokens. Rejects refresh tokens even if
	 * their signature is valid — refresh tokens carry type="refresh" and must never
	 * be used as access credentials.
	 */
	public boolean validateToken(String token) {
		try {
			Claims claims = parseToken(token);
			return !"refresh".equals(claims.get("type"));
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}
}