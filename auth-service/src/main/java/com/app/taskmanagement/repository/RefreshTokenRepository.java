package com.app.taskmanagement.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.app.taskmanagement.entity.RefreshToken;
import com.app.taskmanagement.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user")
	void revokeAllByUser(User user);

	@Modifying
	@Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR r.revoked = true")
	void deleteExpiredAndRevoked(Instant now);

}
