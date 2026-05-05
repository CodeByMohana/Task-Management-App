package com.app.taskmanagement.label.security;

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
 * This filter runs on EVERY incoming HTTP request (before the controller). Its
 * job: find the user's JWT token, validate it, and tell Spring Security who is
 * logged in.
 *
 * WHAT IS A FILTER? Think of it like airport security: every passenger
 * (request) must pass through before reaching the gate (controller). The filter
 * checks their ID (JWT token).
 *
 * OncePerRequestFilter means this filter runs exactly once per request, even if
 * the request is forwarded internally.
 *
 * TOKEN RESOLUTION ORDER (first match wins): 1. X-User-Id header — set by the
 * API gateway after it already validated the JWT. If this header is present, we
 * trust it and skip re-validating the token. This is faster and avoids doing
 * the same work twice.
 *
 * 2. Authorization: Bearer <token> — used by Postman / API clients calling
 * directly.
 *
 * 3. accessToken cookie — used by the browser, set by auth-service on login.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// ── Path 1: Gateway already validated the token ────────────────────
		// The API gateway strips and re-sets X-User-Id after its own JWT check.
		// If this header exists, we trust it — no need to re-parse the token.
		String gatewayUserId = request.getHeader("X-User-Id");
		String gatewayRole = request.getHeader("X-User-Role");

		if (StringUtils.hasText(gatewayUserId)) {
			try {
				int userId = Integer.parseInt(gatewayUserId);
				// Default to MEMBER if role header is missing (shouldn't happen normally)
				String role = StringUtils.hasText(gatewayRole) ? gatewayRole : "MEMBER";
				setAuthentication(userId, role);
			} catch (NumberFormatException ignored) {
				// Malformed header value — fall through to token-based validation
			}
		} else {
			// ── Path 2 & 3: No gateway header — validate token directly ────
			// This path is used when calling label-service directly (dev/testing)
			String token = extractFromHeader(request);
			if (token == null) {
				token = extractFromCookie(request);
			}

			if (token != null && jwtTokenProvider.validateToken(token)) {
				int userId = jwtTokenProvider.extractUserId(token);
				String role = jwtTokenProvider.extractRole(token);
				setAuthentication(userId, role);
			}
			// If no valid token: leave SecurityContext empty.
			// Spring Security will block the request if the endpoint requires auth.
		}

		// Always pass the request to the next filter/controller in the chain
		filterChain.doFilter(request, response);
	}

	/**
	 * Registers the user as "authenticated" in Spring Security's context.
	 *
	 * After this call, any controller can get the userId via:
	 * 
	 * @AuthenticationPrincipal Integer userId
	 *
	 * @param userId the authenticated user's ID (stored as the "principal")
	 * @param role   the user's role (e.g. "MEMBER", "ADMIN")
	 */
	private void setAuthentication(int userId, String role) {
		// UsernamePasswordAuthenticationToken is Spring's standard authenticated-user
		// holder
		// principal = userId (Integer) — who the user is
		// credentials = null — JWT doesn't use passwords here
		// authorities = ROLE_MEMBER etc. — what the user is allowed to do
		var authentication = new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + role)));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	/**
	 * Looks for "Authorization: Bearer <token>" in the request headers. Returns
	 * just the token part (after "Bearer "), or null if not found.
	 */
	private String extractFromHeader(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
			return header.substring(7); // remove "Bearer " prefix (7 characters)
		}
		return null;
	}

	/**
	 * Looks for the "accessToken" cookie in the request. The browser automatically
	 * sends cookies with every request to the same domain. Returns the cookie
	 * value, or null if the cookie isn't present.
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