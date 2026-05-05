package com.app.taskmanagement.service;

import com.app.taskmanagement.entity.User;

public interface RefreshTokenService {

	// Returns the RAW token (stored hashed in DB)
	String createRefreshToken(User user);

	// Validates raw token, returns associated user
	User validateAndGetUser(String rawToken);

	void revokeAllUserTokens(User user);
}