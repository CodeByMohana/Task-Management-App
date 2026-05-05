package com.app.taskmanagement.service;

import java.util.List;

import com.app.taskmanagement.dto.AuthResponse;
import com.app.taskmanagement.dto.ChangePasswordRequest;
import com.app.taskmanagement.dto.LoginRequest;
import com.app.taskmanagement.dto.RegisterRequest;
import com.app.taskmanagement.dto.UpdateProfileRequest;
import com.app.taskmanagement.dto.UserResponse;

public interface AuthService {

	AuthResponse register(RegisterRequest request);

	AuthResponse login(LoginRequest request);

	void logout(String refreshToken);

	AuthResponse refreshToken(String refreshToken);

	UserResponse getProfile(int userId);

	UserResponse updateProfile(int userId, UpdateProfileRequest updatedData);

	void changePassword(int userId, ChangePasswordRequest request);

	void deactivateAccount(int userId);

	UserResponse activateAccount(int userId);

	List<UserResponse> getAllUsers();

	List<UserResponse> searchUsers(String query);

}