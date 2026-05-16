package com.app.taskmanagement.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	@Value("${app.oauth2.authorized-redirect-uri}")
	private String authorizedRedirectUri;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		
		log.error("========================================");
		log.error("OAuth2 Authentication FAILED");
		log.error("Request URL: {}", request.getRequestURL());
		log.error("Query String: {}", request.getQueryString());
		log.error("Exception Type: {}", exception.getClass().getName());
		log.error("Exception Message: {}", exception.getMessage());
		log.error("Full Stack Trace:", exception);
		log.error("========================================");

		String targetUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
				.queryParam("error", exception.getLocalizedMessage())
				.build().toUriString();

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}