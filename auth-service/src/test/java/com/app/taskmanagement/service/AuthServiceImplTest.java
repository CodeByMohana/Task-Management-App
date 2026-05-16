package com.app.taskmanagement.service;

import com.app.taskmanagement.dto.*;
import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.exception.BadRequestException;
import com.app.taskmanagement.exception.DuplicateResourceException;
import com.app.taskmanagement.exception.ResourceNotFoundException;
import com.app.taskmanagement.repository.UserRepository;
import com.app.taskmanagement.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .fullName("John Doe")
                .email("john@example.com")
                .username("johndoe")
                .passwordHash("hashedPassword")
                .role(User.Role.MEMBER)
                .provider("local")
                .isActive(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        testUser = null;
    }

    // ─── Register Tests ───────────────────────────────────────────────

    @Test
    @DisplayName("register - should succeed with valid data")
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setUsername("johndoe");
        request.setPassword("password123");
        request.setOtp("123456");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(otpService.verifyOtp("john@example.com", "123456", "VERIFICATION")).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyInt(), anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        verify(otpService).verifyOtp("john@example.com", "123456", "VERIFICATION");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - should throw DuplicateResourceException when email exists")
    void register_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setUsername("johndoe");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register - should throw DuplicateResourceException when username exists")
    void register_DuplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setUsername("johndoe");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    // ─── Login Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("login - should succeed with valid credentials")
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(anyInt(), anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
    }

    @Test
    @DisplayName("login - should throw BadRequestException for invalid email")
    void login_InvalidEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("login - should throw BadRequestException for deactivated account")
    void login_DeactivatedAccount() {
        testUser.setActive(false);
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("login - should throw BadRequestException for OAuth user attempting local login")
    void login_OAuthUser() {
        testUser.setProvider("google");
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("google"));
    }

    @Test
    @DisplayName("login - should throw BadRequestException for wrong password")
    void login_WrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    // ─── SendOtp Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("sendOtp - VERIFICATION type with new email should succeed")
    void sendOtp_VerificationSuccess() {
        SendOtpRequest request = new SendOtpRequest();
        request.setEmail("new@example.com");
        request.setType("VERIFICATION");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        authService.sendOtp(request);

        verify(otpService).generateAndSendOtp("new@example.com", "VERIFICATION");
    }

    @Test
    @DisplayName("sendOtp - VERIFICATION type with existing email should throw")
    void sendOtp_VerificationDuplicateEmail() {
        SendOtpRequest request = new SendOtpRequest();
        request.setEmail("john@example.com");
        request.setType("VERIFICATION");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.sendOtp(request));
    }

    @Test
    @DisplayName("sendOtp - FORGOT_PASSWORD with unknown email should throw")
    void sendOtp_ForgotPasswordUnknownEmail() {
        SendOtpRequest request = new SendOtpRequest();
        request.setEmail("unknown@example.com");
        request.setType("FORGOT_PASSWORD");

        when(userRepository.existsByEmail("unknown@example.com")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authService.sendOtp(request));
    }

    @Test
    @DisplayName("sendOtp - invalid type should throw BadRequestException")
    void sendOtp_InvalidType() {
        SendOtpRequest request = new SendOtpRequest();
        request.setEmail("john@example.com");
        request.setType("INVALID_TYPE");

        assertThrows(BadRequestException.class, () -> authService.sendOtp(request));
    }

    // ─── ResetPassword Tests ──────────────────────────────────────────

    @Test
    @DisplayName("resetPassword - should succeed for local user with valid OTP")
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");
        request.setNewPassword("newPassword123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(otpService.verifyOtp("john@example.com", "123456", "FORGOT_PASSWORD")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHash");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.resetPassword(request);

        verify(refreshTokenService).revokeAllUserTokens(testUser);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("resetPassword - should throw BadRequestException for OAuth user")
    void resetPassword_OAuthUser() {
        testUser.setProvider("google");
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");
        request.setNewPassword("newPassword123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
    }

    // ─── Logout & Refresh Tests ───────────────────────────────────────

    @Test
    @DisplayName("logout - should revoke all tokens")
    void logout_Success() {
        when(refreshTokenService.validateAndGetUser("refresh-token")).thenReturn(testUser);

        authService.logout("refresh-token");

        verify(refreshTokenService).revokeAllUserTokens(testUser);
    }

    @Test
    @DisplayName("refreshToken - should return new tokens for active user")
    void refreshToken_Success() {
        when(refreshTokenService.validateAndGetUser("refresh-token")).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyInt(), anyString(), anyString())).thenReturn("new-access");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("new-refresh");

        AuthResponse response = authService.refreshToken("refresh-token");

        assertNotNull(response);
        assertEquals("new-access", response.getAccessToken());
        verify(refreshTokenService).revokeAllUserTokens(testUser);
    }

    @Test
    @DisplayName("refreshToken - should throw for deactivated account")
    void refreshToken_DeactivatedAccount() {
        testUser.setActive(false);
        when(refreshTokenService.validateAndGetUser("refresh-token")).thenReturn(testUser);

        assertThrows(BadRequestException.class, () -> authService.refreshToken("refresh-token"));
    }

    // ─── Profile & Account Tests ──────────────────────────────────────

    @Test
    @DisplayName("getProfile - should return user response")
    void getProfile_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        UserResponse response = authService.getProfile(1);

        assertNotNull(response);
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    @DisplayName("getProfile - should throw ResourceNotFoundException for unknown user")
    void getProfile_UserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getProfile(999));
    }

    @Test
    @DisplayName("changePassword - should throw when old password is incorrect")
    void changePassword_WrongOldPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongOld");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOld", "hashedPassword")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.changePassword(1, request));
    }

    @Test
    @DisplayName("changePassword - should throw when new password same as old")
    void changePassword_SameAsOld() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("password123");
        request.setNewPassword("password123");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.changePassword(1, request));
    }

    @Test
    @DisplayName("deactivateAccount - should set active to false and revoke tokens")
    void deactivateAccount_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.deactivateAccount(1);

        assertFalse(testUser.isActive());
        verify(refreshTokenService).revokeAllUserTokens(testUser);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("getAllUsers - should return list of user responses")
    void getAllUsers_Success() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserResponse> result = authService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("john@example.com", result.get(0).getEmail());
    }

    @Test
    @DisplayName("searchUsers - should return matching users")
    void searchUsers_Success() {
        when(userRepository.findByFullNameContainingIgnoreCase("John")).thenReturn(List.of(testUser));

        List<UserResponse> result = authService.searchUsers("John");

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getFullName());
    }
}
