package com.app.taskmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Stored as a hashed value — never store raw tokens in DB
	@Column(nullable = false, unique = true)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	@Builder.Default
	private boolean revoked = false;
}