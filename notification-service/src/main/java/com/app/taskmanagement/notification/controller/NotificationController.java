package com.app.taskmanagement.notification.controller;

import com.app.taskmanagement.notification.dto.NotificationPage;
import com.app.taskmanagement.notification.entity.Notification;
import com.app.taskmanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

//	@GetMapping
//	public ResponseEntity<Page<Notification>> getMyNotifications(Authentication authentication,
//			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
//		String email = authentication.getName();
//		Page<Notification> notifications = notificationService.getUserNotifications(email, PageRequest.of(page, size));
//		return ResponseEntity.ok(notifications);
//	}

	@GetMapping
	public ResponseEntity<Page<Notification>> getMyNotifications(Authentication authentication,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		String email = authentication.getName();
		NotificationPage notificationPage = notificationService.getUserNotifications(email, PageRequest.of(page, size));
		Page<Notification> notifications = notificationPage.toPage();
		return ResponseEntity.ok(notifications);
	}

	@GetMapping("/unread-count")
	public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
		String email = authentication.getName();
		long count = notificationService.getUnreadCount(email);
		return ResponseEntity.ok(Map.of("unreadCount", count));
	}

	@PutMapping("/{id}/read")
	public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
		String email = authentication.getName();
		notificationService.markAsRead(id, email);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/read-all")
	public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
		String email = authentication.getName();
		notificationService.markAllAsRead(email);
		return ResponseEntity.ok().build();
	}
}