package com.app.taskmanagement.workspace.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * This filter runs on EVERY incoming HTTP request — exactly once. Its job:
 * check if the request has a valid JWT, and if so, tell Spring Security who the
 * user is so they can access protected endpoints.
 *
 * Flow: 1. Extract token from "Authorization: Bearer <token>" header 2.
 * Validate the token using JwtTokenProvider 3. If valid → set the user's
 * identity into the SecurityContext 4. Continue to the actual controller
 *
 * If token is missing or invalid, we just skip step 3 — Spring Security will
 * then block access to protected endpoints automatically.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Step 1: Extract the raw token from the Authorization header
		String token = extractToken(request);

		// Step 2 & 3: If token exists and is valid, authenticate the user
		if (token != null && jwtTokenProvider.validateToken(token)) {
			int userId = jwtTokenProvider.extractUserId(token);
			String role = jwtTokenProvider.extractRole(token);

			/*
			 * UsernamePasswordAuthenticationToken represents an authenticated user. -
			 * principal (1st arg): who the user is → we store userId (Integer) -
			 * credentials (2nd arg): password — null because JWT doesn't need it -
			 * authorities (3rd arg): roles → "ROLE_MEMBER", "ROLE_PLATFORM_ADMIN" etc.
			 *
			 * Spring Security uses the "ROLE_" prefix convention for role-based access.
			 */
			var authentication = new UsernamePasswordAuthenticationToken(userId, null,
					List.of(new SimpleGrantedAuthority("ROLE_" + role)));

			// Store the authentication in the SecurityContext for this request
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		// Step 4: Continue to next filter or controller regardless
		filterChain.doFilter(request, response);
	}

	/**
	 * Extracts the JWT from the Authorization header. The header format is: "Bearer
	 * eyJhbGci..." We strip the "Bearer " prefix to get the raw token.
	 */
	private String extractToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
			return header.substring(7); // remove "Bearer " (7 characters)
		}
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if ("accessToken".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}
}