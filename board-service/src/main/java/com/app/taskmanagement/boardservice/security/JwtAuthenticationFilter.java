package com.app.taskmanagement.boardservice.security;

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
 * Intercepts every incoming HTTP request and checks for a valid JWT.
 *
 * Token lookup order: 1. Authorization: Bearer <token> header (Postman / API
 * clients) 2. accessToken cookie (browser — once API gateway is set up)
 *
 * If a valid token is found: → sets the userId as the authenticated principal
 * in Spring Security → controller can then use @AuthenticationPrincipal Integer
 * userId
 *
 * If no valid token: → request proceeds unauthenticated → Spring Security will
 * block it if the endpoint requires authentication
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Step 1: Try to get token from Authorization header first (Postman)
		String token = extractFromHeader(request);

		// Step 2: Fall back to cookie (browser requests)
		if (token == null) {
			token = extractFromCookie(request);
		}

		// Step 3: If a valid token was found, authenticate the user
		if (token != null && jwtTokenProvider.validateToken(token)) {
			int userId = jwtTokenProvider.extractUserId(token);
			String role = jwtTokenProvider.extractRole(token);

			/*
			 * Create an authenticated token for Spring Security. principal = userId
			 * (Integer) — accessible via @AuthenticationPrincipal credentials = null (JWT
			 * doesn't need a password) authorities = user's role with ROLE_ prefix (Spring
			 * Security convention)
			 */
			var authentication = new UsernamePasswordAuthenticationToken(userId, null,
					List.of(new SimpleGrantedAuthority("ROLE_" + role)));

			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		// Always continue — Spring Security handles blocking unauthenticated requests
		filterChain.doFilter(request, response);
	}

	/**
	 * Extract token from "Authorization: Bearer <token>" header. Returns null if
	 * header is missing or not in the correct format.
	 */
	private String extractFromHeader(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}

	/**
	 * Extract token from the "accessToken" httpOnly cookie. Returns null if no
	 * matching cookie found.
	 */
	private String extractFromCookie(HttpServletRequest request) {
		if (request.getCookies() == null)
			return null;
		for (Cookie cookie : request.getCookies()) {
			if ("accessToken".equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
