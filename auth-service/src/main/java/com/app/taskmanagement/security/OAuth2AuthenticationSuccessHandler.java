package com.app.taskmanagement.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.repository.UserRepository;
import com.app.taskmanagement.service.RefreshTokenService;

import java.io.IOException;

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
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		String email = oAuth2User.getAttribute("email");

		if (email == null) {
			Object idObj = oAuth2User.getAttribute("id");
			String providerId = idObj != null ? idObj.toString() : "unknown";
			email = "github_" + providerId + "@github.local";
		}

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("OAuth2 user not found after processing"));

		String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getEmail(),
				user.getRole().name());
		String rawRefreshToken = refreshTokenService.createRefreshToken(user);

		// Set access token as httpOnly cookie
		ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken).httpOnly(true).secure(true)
				.sameSite("None").path("/").maxAge(15 * 60).build();

		response.addHeader("Set-Cookie", accessCookie.toString());

		// Set refresh token as httpOnly cookie
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", rawRefreshToken).httpOnly(true).secure(true)
				.sameSite("None").path("/").maxAge(7 * 24 * 60 * 60).build();

		response.addHeader("Set-Cookie", refreshCookie.toString());

		getRedirectStrategy().sendRedirect(request, response, authorizedRedirectUri);
	}
}