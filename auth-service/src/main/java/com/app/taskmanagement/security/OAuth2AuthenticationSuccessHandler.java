package com.app.taskmanagement.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.repository.UserRepository;
import com.app.taskmanagement.service.RefreshTokenService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;
	private final RefreshTokenService refreshTokenService;

	@Value("${app.oauth2.authorized-redirect-uri}")
	private String authorizedRedirectUri;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {

		try {
			log.info("========================================");
			log.info("OAuth2 Authentication SUCCESS - Starting token generation");

			OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
			log.info("OAuth2 User Principal: {}", oAuth2User.getName());
			log.info("OAuth2 User Attributes: {}", oAuth2User.getAttributes());

			String email = oAuth2User.getAttribute("email");

			// Handle GitHub users without public email
			if (email == null) {
				log.warn("Email is null, checking for GitHub user");
				Object idObj = oAuth2User.getAttribute("id");
				String providerId = idObj != null ? idObj.toString() : "unknown";
				email = "github_" + providerId + "@github.local";
				log.info("Generated email for GitHub user: {}", email);
			}

			final String finalEmail = email;

			log.info("Looking up user by email: {}", finalEmail);

			User user = userRepository.findByEmail(finalEmail).orElseThrow(() -> {
				log.error("User not found for email: {}", finalEmail);
				return new RuntimeException("OAuth2 user not found after processing");
			});

			log.info("User found: ID={}, Email={}, Role={}", user.getUserId(), user.getEmail(), user.getRole());

			// Generate tokens
			log.info("Generating access token...");
			String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getEmail(),
					user.getRole().name());

			log.info("Creating refresh token...");
			String rawRefreshToken = refreshTokenService.createRefreshToken(user);

			// Set access token cookie
			ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken).httpOnly(true).secure(true)
					.sameSite("None").path("/").maxAge(15 * 60).build();
			response.addHeader("Set-Cookie", accessCookie.toString());

			// Set refresh token cookie
			ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", rawRefreshToken).httpOnly(true)
					.secure(true).sameSite("None").path("/").maxAge(7 * 24 * 60 * 60).build();
			response.addHeader("Set-Cookie", refreshCookie.toString());

			log.info("Cookies set successfully");
			log.info("Redirecting to: {}", authorizedRedirectUri);
			log.info("========================================");

			getRedirectStrategy().sendRedirect(request, response, authorizedRedirectUri);

		} catch (Exception e) {
			log.error("========================================");
			log.error("CRITICAL ERROR in OAuth2 Success Handler");
			log.error("Error Type: {}", e.getClass().getName());
			log.error("Error Message: {}", e.getMessage());
			log.error("Full Stack Trace:", e);
			log.error("========================================");
			throw e;
		}
	}
}