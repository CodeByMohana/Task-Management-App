package com.app.taskmanagement.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
	private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/auth/register",
								"/api/auth/login",
								"/api/auth/refresh",
								"/api/auth/send-otp",
								"/api/auth/forgot-password/reset",
								"/oauth2/**",
								"/login/**",  // Critical for OAuth2 callback
								"/api/auth/users/**",
								"/swagger-ui/**",
								"/api-docs/**",
								"/v3/api-docs/**"
						).permitAll()
						.requestMatchers("/api/admin/**").hasRole("PLATFORM_ADMIN")
						.anyRequest().authenticated()
				)
				.oauth2Login(oauth2 -> oauth2
						// DO NOT override the default paths - Spring Security expects:
						// Authorization: /oauth2/authorization/{registrationId}
						// Callback: /login/oauth2/code/{registrationId}
						.userInfoEndpoint(u -> u.userService(customOAuth2UserService))
						.successHandler(oAuth2SuccessHandler)
						.failureHandler(oAuth2FailureHandler)
				)
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}