package com.app.taskmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

	@NotBlank
	private String fullName;

	@Email
	@NotBlank
	private String email;

	@NotBlank
	@Size(min = 3, max = 30)
	private String username;

	@NotBlank
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String password;
<<<<<<< Updated upstream
=======

	@NotBlank
	private String otp;
>>>>>>> Stashed changes
}