package com.app.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.taskmanagement.entity.RefreshToken;
import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.exception.TokenException;
import com.app.taskmanagement.repository.RefreshTokenRepository;
import com.app.taskmanagement.security.JwtTokenProvider;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public String createRefreshToken(User user) {
		String rawToken = UUID.randomUUID().toString();
		// BCrypt the token before storing — raw token only lives in the HTTP response
		String tokenHash = passwordEncoder.encode(rawToken);

		RefreshToken refreshToken = RefreshToken.builder().tokenHash(tokenHash).user(user)
				.expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpiryMs())).build();

		refreshTokenRepository.save(refreshToken);
		return rawToken;
	}

	@Override
	@Transactional(readOnly = true)
	public User validateAndGetUser(String rawToken) {
		// We can't query by hash directly since BCrypt is non-deterministic.
		// In production, use HMAC-SHA256 hash for lookups, BCrypt only for storage.
		// For now: find by matching (acceptable for low token volume)
		return refreshTokenRepository.findAll().stream()
				.filter(rt -> !rt.isRevoked() && rt.getExpiresAt().isAfter(Instant.now())
						&& passwordEncoder.matches(rawToken, rt.getTokenHash()))
				.findFirst().map(RefreshToken::getUser)
				.orElseThrow(() -> new TokenException("Invalid or expired refresh token"));
	}

	@Override
	@Transactional
	public void revokeAllUserTokens(User user) {
		refreshTokenRepository.revokeAllByUser(user);
	}
}