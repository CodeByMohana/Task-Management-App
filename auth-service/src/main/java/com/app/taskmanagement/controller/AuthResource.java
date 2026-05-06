package com.app.taskmanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.app.taskmanagement.dto.AuthResponse;
import com.app.taskmanagement.dto.ChangePasswordRequest;
import com.app.taskmanagement.dto.LoginRequest;
import com.app.taskmanagement.dto.RefreshTokenRequest;
import com.app.taskmanagement.dto.RegisterRequest;
import com.app.taskmanagement.dto.UpdateProfileRequest;
import com.app.taskmanagement.dto.UserResponse;
<<<<<<< Updated upstream
=======
import com.app.taskmanagement.dto.SendOtpRequest;
import com.app.taskmanagement.dto.ResetPasswordRequest;
>>>>>>> Stashed changes
import com.app.taskmanagement.service.AuthService;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and user management")
public class AuthResource {

	private final AuthService authService;

	// ─── Public Endpoints ────────────────────────────────────────────────────

<<<<<<< Updated upstream
=======
	@PostMapping("/send-otp")
	@Operation(summary = "Send OTP for verification or password reset")
	public ResponseEntity<Void> sendOtp(@Valid @RequestBody SendOtpRequest request) {
		authService.sendOtp(request);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/forgot-password/reset")
	@Operation(summary = "Reset password using OTP")
	public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
		return ResponseEntity.ok().build();
	}

>>>>>>> Stashed changes
	@PostMapping("/register")
	@Operation(summary = "Register a new user")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	@Operation(summary = "Login with email and password")
	public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		AuthResponse auth = authService.login(request);

		// Set cookies
		addAccessTokenCookie(response, auth.getAccessToken());
		addRefreshTokenCookie(response, auth.getRefreshToken());

		// Return only user info in body — no tokens exposed
		return ResponseEntity.ok(auth.getUser());
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh access token")
	public ResponseEntity<AuthResponse> refresh(
			@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {

		if (refreshToken == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		AuthResponse auth = authService.refreshToken(refreshToken);

		addRefreshTokenCookie(response, auth.getRefreshToken());

		addAccessTokenCookie(response, auth.getAccessToken());

		return ResponseEntity.ok().build();
	}

	@PostMapping("/logout")
	@Operation(summary = "Logout and revoke refresh token")
	public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken,
			HttpServletResponse response) {

		if (refreshToken != null) {
			authService.logout(refreshToken);
		}

		deleteAccessTokenCookie(response);
		deleteRefreshTokenCookie(response);
		return ResponseEntity.noContent().build();
	}

	// ─── Authenticated Endpoints ─────────────────────────────────────────────

	@GetMapping("/profile")
	@Operation(summary = "Get current user profile", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal Integer userId) {
		return ResponseEntity.ok(authService.getProfile(userId));
	}

	@PutMapping("/profile")
	@Operation(summary = "Update current user profile", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal Integer userId,
			@Valid @RequestBody UpdateProfileRequest request) {
		return ResponseEntity.ok(authService.updateProfile(userId, request));
	}

	@PutMapping("/change-password")
	@Operation(summary = "Change password (local accounts only)", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Integer userId,
			@Valid @RequestBody ChangePasswordRequest request) {

		authService.changePassword(userId, request);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/users/{id}")
	@Operation(summary = "Get user profile by ID", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<UserResponse> getUserById(@PathVariable int id) {
		return ResponseEntity.ok(authService.getProfile(id));
	}

	@DeleteMapping("/account")
	@Operation(summary = "Deactivate own account", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deactivateAccount(@AuthenticationPrincipal Integer userId) {
		authService.deactivateAccount(userId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/users/search")
	@Operation(summary = "Search users by name", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String query) {
		return ResponseEntity.ok(authService.searchUsers(query));
	}

	// ─── Admin Endpoints ─────────────────────────────────────────────────────

	@DeleteMapping("/admin/users/{userId}")
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	@Operation(summary = "Admin: deactivate any user", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> adminDeactivateUser(@PathVariable int userId) {
		authService.deactivateAccount(userId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/admin/users/{userId}/activate")
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	@Operation(summary = "Admin: reactivate a deactivated user", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<UserResponse> adminActivateUser(@PathVariable int userId) {
		return ResponseEntity.ok(authService.activateAccount(userId));
	}

	@GetMapping("/admin/users")
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	@Operation(summary = "Admin: get all users", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<UserResponse>> adminGetAllUsers() {
		return ResponseEntity.ok(authService.getAllUsers());
	}

	// ─── Helper Methods ─────────────────────────────────────────────────────
	private void addAccessTokenCookie(HttpServletResponse response, String token) {
		Cookie cookie = new Cookie("accessToken", token);
		cookie.setHttpOnly(true);
		cookie.setSecure(false); // set true in production
		cookie.setPath("/");
		cookie.setMaxAge(15 * 60);
		cookie.setAttribute("SameSite", "Lax");
		response.addCookie(cookie);
	}

	private void addRefreshTokenCookie(HttpServletResponse response, String token) {
		Cookie cookie = new Cookie("refreshToken", token);
		cookie.setHttpOnly(true);
		cookie.setSecure(false);
		cookie.setPath("/");
		cookie.setMaxAge(7 * 24 * 60 * 60);
		cookie.setAttribute("SameSite", "Lax");
		response.addCookie(cookie);
	}

	private void deleteAccessTokenCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie("accessToken", null);
		cookie.setHttpOnly(true);
		cookie.setSecure(false); // set true in production
		cookie.setPath("/");
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

	private void deleteRefreshTokenCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie("refreshToken", null);
		cookie.setHttpOnly(true);
		cookie.setSecure(false);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

}