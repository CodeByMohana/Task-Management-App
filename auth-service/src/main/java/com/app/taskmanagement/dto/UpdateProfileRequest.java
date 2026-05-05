package com.app.taskmanagement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

	private String fullName;

	@Size(min = 3, max = 30)
	private String username;

	private String avatarUrl;
}
