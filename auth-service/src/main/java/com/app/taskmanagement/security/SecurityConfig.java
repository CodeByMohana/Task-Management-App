package com.app.taskmanagement.security;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				// OAuth2 REQUIRES a session to store the CSRF state token between
				// the authorization redirect and the provider callback.
				// STATELESS breaks this — IF_REQUIRED creates a session only when needed.
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(auth -> auth
<<<<<<< Updated upstream
						.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/oauth2/**",
=======
						.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/send-otp", "/api/auth/forgot-password/reset", "/oauth2/**",
>>>>>>> Stashed changes
								"/login/**", "/api/auth/users/**", // Spring Security OAuth2 internal callback path
								"/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**")
						.permitAll().requestMatchers("/api/admin/**").hasRole("PLATFORM_ADMIN").anyRequest()
						.authenticated())
				.oauth2Login(oauth2 -> oauth2.authorizationEndpoint(a -> a.baseUri("/oauth2/authorize"))
						.redirectionEndpoint(r -> r.baseUri("/oauth2/callback/*"))
						.userInfoEndpoint(u -> u.userService(customOAuth2UserService))
						.successHandler(oAuth2SuccessHandler))
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}