package com.app.taskmanagement.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int userId;

	@Column(nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true)
	private String email;

	// Nullable: OAuth users won't have a password
	private String passwordHash;

	@Column(unique = true)
	private String username;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	private String avatarUrl;

	// "local" | "google" | "github"
	@Column(nullable = false)
	private String provider;

	// Provider's user ID (for OAuth users)
	private String providerId;

	@Column(nullable = false)
	@Builder.Default
	private boolean isActive = true;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	public enum Role {
		MEMBER, BOARD_OWNER, PLATFORM_ADMIN
	}

}
