package com.app.taskmanagement.workspace.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Handles JWT token validation in workspace-service.
 *
 * IMPORTANT: This service does NOT generate tokens — only auth-service does
 * that. We only need to VERIFY that an incoming token is valid and extract the
 * user info from it.
 *
 * The JWT secret must be identical to the one in auth-service — that's how both
 * services can work with the same tokens.
 */
@Component
public class JwtTokenProvider {

	/**
	 * The secret key used to verify the token's signature. Both auth-service and
	 * workspace-service share this key via environment variable.
	 */
	private final SecretKey secretKey;

	public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
		// Decode the base64 secret string into a cryptographic key
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
	}

	/**
	 * Parses and validates a JWT token. Throws JwtException if the token is
	 * expired, tampered, or malformed.
	 *
	 * @param token raw JWT string (without "Bearer " prefix)
	 * @return Claims — the payload data inside the token
	 */
	public Claims parseToken(String token) {
		return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
	}

	/**
	 * Extracts the userId from the token's subject field. In auth-service, we set
	 * subject = String.valueOf(userId) when creating the token.
	 */
	public int extractUserId(String token) {
		return Integer.parseInt(parseToken(token).getSubject());
	}

	/**
	 * Extracts the user's role from token claims. Example: "MEMBER", "BOARD_OWNER",
	 * "PLATFORM_ADMIN"
	 */
	public String extractRole(String token) {
		return (String) parseToken(token).get("role");
	}

	/**
	 * Checks if a token is valid — not expired, correct signature, correct format.
	 *
	 * @return true if valid, false otherwise (never throws)
	 */
	public boolean validateToken(String token) {
		try {
			parseToken(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			// Token is invalid — we return false instead of throwing
			// so the filter can handle it gracefully
			return false;
		}
	}
}