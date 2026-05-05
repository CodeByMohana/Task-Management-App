package com.app.taskmanagement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.app.taskmanagement.entity.User;

@Data
@Builder
public class UserResponse {
	private long userId;
	private String fullName;
	private String email;
	private String username;
	private String role;
	private String avatarUrl;
	private String provider;
	private boolean isActive;
	private LocalDateTime createdAt;

	public static UserResponse from(User user) {
		return UserResponse.builder().userId(user.getUserId()).fullName(user.getFullName()).email(user.getEmail())
				.username(user.getUsername()).role(user.getRole().name()).avatarUrl(user.getAvatarUrl())
				.provider(user.getProvider()).isActive(user.isActive()).createdAt(user.getCreatedAt()).build();
	}
}