package com.app.taskmanagement.boardservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Validates JWT tokens in board-service.
 *
 * This service does NOT generate tokens — only auth-service does. We only
 * VERIFY that a token is valid and extract user info from it.
 *
 * The JWT secret must be IDENTICAL to the one in auth-service. Both services
 * use the same secret to sign/verify the same tokens.
 */
@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;

	public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
		// Decode base64 secret into a cryptographic key
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
	 * Extract userId from the token subject field. auth-service sets subject =
	 * String.valueOf(userId) when creating the token.
	 */
	public int extractUserId(String token) {
		return Integer.parseInt(parseToken(token).getSubject());
	}

	/**
	 * Extract the user's role from the token claims. Example values: "MEMBER",
	 * "BOARD_OWNER", "PLATFORM_ADMIN"
	 */
	public String extractRole(String token) {
		return (String) parseToken(token).get("role");
	}

	/**
	 * Returns true if token is valid (correct signature + not expired). Never
	 * throws — returns false on any error so the filter handles it gracefully.
	 */
	public boolean validateToken(String token) {
		try {
			parseToken(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}
}