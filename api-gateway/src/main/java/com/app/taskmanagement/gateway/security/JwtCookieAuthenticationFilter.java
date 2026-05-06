package com.app.taskmanagement.gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter implements GlobalFilter, Ordered {

	private static final List<String> PUBLIC_PATHS = List.of("/api/auth/login", "/api/auth/register",
			"/api/auth/refresh", "/api/auth/logout", "/api/auth/send-otp", "/api/auth/forgot-password/reset",
			"/swagger-ui.html", "/auth/api-docs", "/workspace/api-docs",
			"/board/api-docs", "/card/api-docs", "/comment/api-docs", "/label/api-docs", "/notification/api-docs");

	private static final List<String> PUBLIC_PREFIXES = List.of("/oauth2/", "/swagger-ui/", "/v3/api-docs/",
			"/auth/api-docs/", "/workspace/api-docs/", "/board/api-docs/", "/card/api-docs/", "/comment/api-docs",
			"/label/api-docs", "/notification/api-docs");

	private boolean isPublicPath(String path) {
		if (PUBLIC_PATHS.contains(path)) {
			return true;
		}

		if ("/auth/swagger-ui.html".equals(path) || "/workspace/swagger-ui.html".equals(path)
				|| "/board/swagger-ui.html".equals(path) || "/card/swagger-ui.html".equals(path)) {
			return true;
		}

		return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
	}

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getURI().getPath();

		if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod()) || isPublicPath(path)) {
			return chain.filter(exchange);
		}

		String token = extractAccessToken(exchange);
		if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
			return unauthorized(exchange);
		}

		String userId = String.valueOf(jwtTokenProvider.extractUserId(token));
		String role = jwtTokenProvider.extractRole(token);

		ServerHttpRequest mutatedRequest = exchange.getRequest().mutate().headers(headers -> {
			headers.remove("X-User-Id");
			headers.remove("X-User-Role");
			headers.set("X-User-Id", userId);
			if (StringUtils.hasText(role)) {
				headers.set("X-User-Role", role);
			}
			headers.setBearerAuth(token);
		}).build();

		return chain.filter(exchange.mutate().request(mutatedRequest).build());
	}

	private String extractAccessToken(ServerWebExchange exchange) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);

		if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
			return authorization.substring(7);
		}

		var accessTokenCookie = exchange.getRequest().getCookies().getFirst("accessToken");
		return accessTokenCookie != null ? accessTokenCookie.getValue() : null;
	}

	private Mono<Void> unauthorized(ServerWebExchange exchange) {
		byte[] body = "{\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);

		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

		return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
	}

	@Override
	public int getOrder() {
		return -100;
	}
}
