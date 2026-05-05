package com.app.taskmanagement.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.repository.UserRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserRepository userRepository;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);
		String provider = userRequest.getClientRegistration().getRegistrationId(); // "google" or "github"

		Map<String, Object> attributes = oAuth2User.getAttributes();
		String email = extractEmail(provider, attributes);
		String fullName = extractFullName(provider, attributes);
		String avatarUrl = extractAvatarUrl(provider, attributes);
		String providerId = String
				.valueOf(attributes.get("sub") != null ? attributes.get("sub") : attributes.get("id"));

		userRepository.findByEmail(email)
				.orElseGet(() -> userRepository.save(User.builder().email(email).fullName(fullName)
						.username(generateUsername(email)).avatarUrl(avatarUrl).provider(provider)
						.providerId(providerId).role(User.Role.MEMBER).isActive(true).build()));

		return oAuth2User;
	}

	private String extractEmail(String provider, Map<String, Object> attrs) {
		if ("github".equals(provider)) {
			Object emailObj = attrs.get("email");

			if (emailObj != null) {
				return emailObj.toString();
			}

			Object idObj = attrs.get("id");
			String providerId = idObj != null ? idObj.toString() : "unknown";

			return provider + "_" + providerId + "@github.local";
		}

		Object emailObj = attrs.get("email");
		return emailObj != null ? emailObj.toString() : null;
	}

	private String extractFullName(String provider, Map<String, Object> attrs) {
		return "github".equals(provider) ? (String) attrs.getOrDefault("name", attrs.get("login"))
				: (String) attrs.get("name");
	}

	private String extractAvatarUrl(String provider, Map<String, Object> attrs) {
		return "github".equals(provider) ? (String) attrs.get("avatar_url") : (String) attrs.get("picture");
	}

	private String generateUsername(String email) {
		// e.g. john.doe@gmail.com → john.doe_abc123
		String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
		return base + "_" + Integer.toHexString((int) (Math.random() * 0xFFFF));
	}
}
