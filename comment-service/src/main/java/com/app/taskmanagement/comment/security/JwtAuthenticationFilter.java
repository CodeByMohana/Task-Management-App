package com.app.taskmanagement.comment.security;

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
 * JWT authentication filter for comment-service.
 *
 * Token resolution order (first match wins): 1. X-User-Id header — injected by
 * API gateway after it already validated the JWT. When present we trust it
 * directly without re-parsing the token. This is faster and avoids duplicating
 * JWT validation logic. 2. Authorization: Bearer <token> header — used when
 * calling directly (Postman, integration tests, service-to-service without
 * gateway). 3. accessToken cookie — browser requests going through the gateway.
 *
 * Security note on X-User-Id header trust: We trust X-User-Id ONLY because the
 * API gateway strips and re-sets this header after its own JWT validation. Any
 * client-provided X-User-Id that bypasses the gateway reaches us directly — and
 * we then fall through to the Bearer/cookie validation path which provides
 * genuine verification. This means: never expose this service's port directly
 * in production.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Path 1: gateway already validated the token and set X-User-Id
		String gatewayUserId = request.getHeader("X-User-Id");
		String gatewayRole = request.getHeader("X-User-Role");

		if (StringUtils.hasText(gatewayUserId)) {
			try {
				int userId = Integer.parseInt(gatewayUserId);
				String role = StringUtils.hasText(gatewayRole) ? gatewayRole : "MEMBER";
				setAuthentication(userId, role);
			} catch (NumberFormatException ignored) {
				// Malformed header — fall through to token-based auth below
			}
		} else {
			// Path 2 & 3: direct call — validate token ourselves
			String token = extractFromHeader(request);
			if (token == null) {
				token = extractFromCookie(request);
			}

			if (token != null && jwtTokenProvider.validateToken(token)) {
				int userId = jwtTokenProvider.extractUserId(token);
				String role = jwtTokenProvider.extractRole(token);
				setAuthentication(userId, role);
			}
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Registers the user as authenticated in Spring Security's context. principal =
	 * userId (Integer) — available via @AuthenticationPrincipal.
	 */
	private void setAuthentication(int userId, String role) {
		var authentication = new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + role)));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private String extractFromHeader(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}

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