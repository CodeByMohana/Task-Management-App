package com.app.taskmanagement.workspace.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.annotation.PostConstruct;

/**
 * Spring Security configuration for workspace-service.
 *
 * Key decisions: - STATELESS session: we don't store sessions on the server —
 * JWT handles identity - CSRF disabled: CSRF protection is for browsers with
 * cookies; we use JWT headers - @EnableMethodSecurity: allows @PreAuthorize on
 * individual methods in controllers
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// Disable CSRF — not needed for stateless JWT APIs
				.csrf(AbstractHttpConfigurer::disable)

				// Don't create HTTP sessions — each request must carry its own JWT
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)

				.authorizeHttpRequests(auth -> auth
						// Allow Swagger UI without authentication (helpful during development)
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**")
						.permitAll()
						// Allow public workspace browsing without login
						.requestMatchers("/api/workspaces/public").permitAll()
						// Everything else requires a valid JWT
						.anyRequest().authenticated())

				// Register our JWT filter BEFORE Spring's default username/password filter
				// This ensures JWT is checked first on every request
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}