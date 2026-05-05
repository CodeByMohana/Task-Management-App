package com.app.taskmanagement.notification.service;

import com.app.taskmanagement.notification.dto.NotificationEvent;
import com.app.taskmanagement.notification.entity.Notification;
import com.app.taskmanagement.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
	private final NotificationRepository notificationRepository;
	private final EmailService emailService;
	private final RestTemplate restTemplate;

	@Transactional
	public void processNotification(NotificationEvent event) {
		
		// If email is missing but userId is present, fetch the user details from auth-service
		if ((event.getRecipientEmail() == null || event.getRecipientEmail().isEmpty()) && event.getRecipientUserId() != null) {
			try {
				ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
					"http://auth-service/api/auth/users/" + event.getRecipientUserId(),
					HttpMethod.GET,
					null,
					new ParameterizedTypeReference<Map<String, Object>>() {}
				);
				
				if (response.getBody() != null) {
					event.setRecipientEmail((String) response.getBody().get("email"));
					event.setRecipientName((String) response.getBody().get("fullName"));
				}
			} catch (Exception e) {
				log.warn("Could not fetch user details for userId: {}", event.getRecipientUserId(), e);
				if (event.getRecipientEmail() == null) {
					event.setRecipientEmail("user_" + event.getRecipientUserId() + "@flowboard.local");
				}
			}
		}

		// Also handle triggeredByUserId if name is missing
		if ((event.getTriggeredBy() == null || event.getTriggeredBy().isEmpty()) && event.getTriggeredByUserId() != null) {
			try {
				ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
					"http://auth-service/api/auth/users/" + event.getTriggeredByUserId(),
					HttpMethod.GET,
					null,
					new ParameterizedTypeReference<Map<String, Object>>() {}
				);
				
				if (response.getBody() != null) {
					event.setTriggeredBy((String) response.getBody().get("fullName"));
				}
			} catch (Exception e) {
				log.warn("Could not fetch user details for triggeredByUserId: {}", event.getTriggeredByUserId());
				event.setTriggeredBy("Someone");
			}
		}

		log.info("Processing notification event: {} for user: {}", event.getEventType(), event.getRecipientEmail());

		// Save notification to database
		Notification notification = Notification.builder().eventType(event.getEventType())
				.recipientEmail(event.getRecipientEmail()).recipientName(event.getRecipientName())
				.subject(event.getSubject()).message(event.getMessage()).entityId(event.getEntityId())
				.entityType(event.getEntityType()).workspaceId(event.getWorkspaceId())
				.triggeredBy(event.getTriggeredBy()).status(Notification.NotificationStatus.PENDING)
				.isRead(false).build();

		notification = notificationRepository.save(notification);

		// Send email
		try {
			emailService.sendEmail(event.getRecipientEmail(), event.getSubject(), event.getMessage());

			notification.setStatus(Notification.NotificationStatus.SENT);
			notification.setSentAt(LocalDateTime.now());
			log.info("Notification sent successfully to: {}", event.getRecipientEmail());
		} catch (Exception e) {
			notification.setStatus(Notification.NotificationStatus.FAILED);
			notification.setErrorMessage(e.getMessage());
			log.error("Failed to send notification to: {}", event.getRecipientEmail(), e);
		}

		notificationRepository.save(notification);
	}

	public Page<Notification> getUserNotifications(String email, Pageable pageable) {
		return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email, pageable);
	}

	public long getUnreadCount(String email) {
		return notificationRepository.countByRecipientEmailAndIsRead(email, false);
	}

	@Transactional
	public void markAsRead(Long id, String email) {
		notificationRepository.findById(id).ifPresent(n -> {
			if (n.getRecipientEmail().equals(email)) {
				n.setIsRead(true);
				notificationRepository.save(n);
			}
		});
	}

	@Transactional
	public void markAllAsRead(String email) {
		notificationRepository.markAllAsReadByEmail(email);
	}
}