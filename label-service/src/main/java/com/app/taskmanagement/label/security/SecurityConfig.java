package com.app.taskmanagement.label.security;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configures Spring Security for label-service.
 *
 * Key settings explained for beginners:
 *
 * CSRF disabled: CSRF (Cross-Site Request Forgery) protection is for apps that
 * use browser form submissions with cookies. Our API uses JWT tokens, so CSRF
 * protection is not needed and would break our API calls.
 *
 * STATELESS sessions: Normally, a server creates a "session" for each logged-in
 * user and stores it in memory. With JWT, the token itself carries the user's
 * identity — no server-side session needed. STATELESS = no sessions.
 *
 * @EnableMethodSecurity: Allows us to use @PreAuthorize on controller methods
 *                        in the future.
 *                        Example: @PreAuthorize("hasRole('ADMIN')") on a
 *                        method.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	// Our custom JWT filter — injected automatically by Spring
	private final JwtAuthenticationFilter jwtAuthFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Swagger UI — open so developers can explore the API without a token
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**")
						.permitAll()
						// Every other endpoint requires a valid JWT
						.anyRequest().authenticated())
				// Run our JWT filter BEFORE Spring's default authentication filter.
				// This way Spring Security knows who the user is before deciding if they're
				// allowed.
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}