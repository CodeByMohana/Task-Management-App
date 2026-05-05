package com.app.taskmanagement.service;

import com.app.taskmanagement.dto.AuthResponse;
import com.app.taskmanagement.dto.ChangePasswordRequest;
import com.app.taskmanagement.dto.LoginRequest;
import com.app.taskmanagement.dto.RegisterRequest;
import com.app.taskmanagement.dto.UpdateProfileRequest;
import com.app.taskmanagement.dto.UserResponse;
import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.exception.BadRequestException;
import com.app.taskmanagement.exception.DuplicateResourceException;
import com.app.taskmanagement.exception.ResourceNotFoundException;
import com.app.taskmanagement.repository.UserRepository;
import com.app.taskmanagement.security.JwtTokenProvider;
import com.app.taskmanagement.service.AuthService;
import com.app.taskmanagement.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenService refreshTokenService;
	private final JwtTokenProvider jwtTokenProvider;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateResourceException("Email already registered");
		}
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new DuplicateResourceException("Username already taken");
		}

		User user = User.builder().fullName(request.getFullName()).email(request.getEmail())
				.username(request.getUsername()).passwordHash(passwordEncoder.encode(request.getPassword()))
				.role(User.Role.MEMBER).provider("local").isActive(true).build();

		userRepository.save(user);
		return buildAuthResponse(user);
	}

	@Override
	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BadRequestException("Invalid email or password"));

		if (!user.isActive()) {
			throw new BadRequestException("Account is deactivated");
		}

		if (!"local".equals(user.getProvider())) {
			throw new BadRequestException("Please log in with " + user.getProvider());
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new BadRequestException("Invalid email or password");
		}

		return buildAuthResponse(user);
	}

	@Override
	@Transactional
	public void logout(String rawRefreshToken) {
		User user = refreshTokenService.validateAndGetUser(rawRefreshToken);
		refreshTokenService.revokeAllUserTokens(user);
	}

	@Override
	@Transactional
	public AuthResponse refreshToken(String rawRefreshToken) {
		User user = refreshTokenService.validateAndGetUser(rawRefreshToken);

		if (!user.isActive()) {
			throw new BadRequestException("Account is deactivated");
		}

		refreshTokenService.revokeAllUserTokens(user);
		return buildAuthResponse(user);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getProfile(int userId) {
		return UserResponse.from(findUserById(userId));
	}

	@Override
	@Transactional
	public UserResponse updateProfile(int userId, UpdateProfileRequest request) {
		User user = findUserById(userId);

		if (StringUtils.hasText(request.getUsername()) && !request.getUsername().equals(user.getUsername())) {
			if (userRepository.existsByUsername(request.getUsername())) {
				throw new DuplicateResourceException("Username already taken");
			}
			user.setUsername(request.getUsername());
		}
		if (StringUtils.hasText(request.getFullName())) {
			user.setFullName(request.getFullName());
		}
		if (StringUtils.hasText(request.getAvatarUrl())) {
			user.setAvatarUrl(request.getAvatarUrl());
		}

		return UserResponse.from(userRepository.save(user));
	}

	@Override
	@Transactional
	public void changePassword(int userId, ChangePasswordRequest request) {
		User user = findUserById(userId);

		if (!"local".equals(user.getProvider())) {
			throw new BadRequestException("OAuth users cannot change password");
		}
		if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
			throw new BadRequestException("Current password is incorrect");
		}
		if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
			throw new BadRequestException("New password must differ from current password");
		}

		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		refreshTokenService.revokeAllUserTokens(user);
		userRepository.save(user);
	}

	@Override
	@Transactional
	public void deactivateAccount(int userId) {
		User user = findUserById(userId);
		user.setActive(false);
		refreshTokenService.revokeAllUserTokens(user);
		userRepository.save(user);
	}

	@Override
	@Transactional
	public UserResponse activateAccount(int userId) {
		User user = findUserById(userId);
		user.setActive(true);
		return UserResponse.from(userRepository.save(user));
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream().map(UserResponse::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserResponse> searchUsers(String query) {
		return userRepository.findByFullNameContainingIgnoreCase(query).stream().map(UserResponse::from).toList();
	}

	// ─── Private Helpers ─────────────────────────────────────────────────────

	private User findUserById(int userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
	}

	private AuthResponse buildAuthResponse(User user) {
		String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getEmail(),
				user.getRole().name());
		String rawRefreshToken = refreshTokenService.createRefreshToken(user);

		return AuthResponse.builder().accessToken(accessToken).refreshToken(rawRefreshToken).tokenType("Bearer")
				.user(UserResponse.from(user)).build();
	}
}
