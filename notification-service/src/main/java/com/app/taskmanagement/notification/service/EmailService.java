package com.app.taskmanagement.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.username:noreply@taskmanagement.com}")
	private String fromEmail;

	/**
	 * Sends an HTML email.
	 */
	public void sendHtmlEmail(String to, String subject, String htmlBody) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

			helper.setFrom(fromEmail);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlBody, true); // true = isHtml

			mailSender.send(mimeMessage);
			log.info("HTML email sent successfully to: {}", to);
		} catch (MessagingException e) {
			log.error("Failed to send HTML email to: {}", to, e);
			throw new RuntimeException("Email sending failed", e);
		}
	}

	/**
	 * Sends a plain-text email (kept for backward compatibility).
	 */
	public void sendEmail(String to, String subject, String body) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

			helper.setFrom(fromEmail);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(body, false);

			mailSender.send(mimeMessage);
			log.info("Email sent successfully to: {}", to);
		} catch (MessagingException e) {
			log.error("Failed to send email to: {}", to, e);
			throw new RuntimeException("Email sending failed", e);
		}
	}
}