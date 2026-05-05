package com.app.taskmanagement.config;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.app.taskmanagement.repository.RefreshTokenRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

	private final RefreshTokenRepository refreshTokenRepository;

	// Runs every day at 2 AM
	@Scheduled(cron = "0 0 2 * * *")
	@Transactional
	public void cleanExpiredTokens() {
		refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
		log.info("Expired and revoked refresh tokens cleaned up");
	}
}