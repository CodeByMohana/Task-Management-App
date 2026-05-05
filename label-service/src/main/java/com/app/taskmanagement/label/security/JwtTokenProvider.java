package com.app.taskmanagement.label.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Handles reading and validating JWT tokens.
 *
 * WHAT IS A JWT? A JSON Web Token is a string that looks like:
 * xxxxx.yyyyy.zzzzz - Part 1 (header): algorithm used to sign the token - Part
 * 2 (payload): data stored inside — userId, role, expiry - Part 3 (signature):
 * proves the token wasn't tampered with
 *
 * WHY DOES label-service NEED THIS? The API gateway forwards every request here
 * with the JWT token still attached. We validate the token ourselves to know
 * WHO is making the request. This service NEVER generates tokens — only
 * auth-service does that.
 *
 * IMPORTANT: The jwt.secret here MUST match auth-service's jwt.secret exactly.
 * If they differ, every token will fail validation with "Invalid signature".
 */
@Component
public class JwtTokenProvider {

	// The secret key used to verify the token's signature.
	// Loaded from application.properties: jwt.secret=...
	private final SecretKey secretKey;

	public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
		// Keys.hmacShaKeyFor converts the raw string into a proper cryptographic key
		// object
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
	}

	/**
	 * Reads and verifies the token, returning its payload (claims). Claims are
	 * key-value pairs stored inside the token. Example claims: { "sub": "42",
	 * "role": "MEMBER", "type": "access" }
	 *
	 * Throws JwtException if the token is expired, tampered with, or invalid.
	 */
	public Claims parseToken(String token) {
		return Jwts.parser().verifyWith(secretKey) // verify the signature using our secret
				.build().parseSignedClaims(token).getPayload(); // return just the data (not the header/signature)
	}

	/**
	 * Gets the userId stored in the token's "sub" (subject) field. auth-service
	 * stores the userId as a string in "sub", so we parse it to int. Example:
	 * "sub": "42" → returns 42
	 */
	public int extractUserId(String token) {
		return Integer.parseInt(parseToken(token).getSubject());
	}

	/**
	 * Gets the user's role from the token. Example: "role": "MEMBER" or "role":
	 * "ADMIN" Used by SecurityConfig to enforce role-based access control.
	 */
	public String extractRole(String token) {
		return (String) parseToken(token).get("role");
	}

	/**
	 * Checks if a token is valid AND is an access token (not a refresh token).
	 *
	 * WHY REJECT REFRESH TOKENS? The system has two token types: - Access tokens
	 * (type="access"): short-lived (24h), used for API calls - Refresh tokens
	 * (type="refresh"): long-lived, used ONLY to get new access tokens
	 *
	 * If someone sends a refresh token to an API endpoint, that's wrong and
	 * potentially a security issue. We reject it explicitly.
	 *
	 * Returns false (invalid) if: - Token is expired - Token signature doesn't
	 * match our secret (tampered) - Token type is "refresh" instead of "access"
	 */
	public boolean validateToken(String token) {
		try {
			Claims claims = parseToken(token);
			return !"refresh".equals(claims.get("type"));
		} catch (JwtException | IllegalArgumentException e) {
			// Any JWT error means the token is invalid — don't authenticate the user
			return false;
		}
	}
}