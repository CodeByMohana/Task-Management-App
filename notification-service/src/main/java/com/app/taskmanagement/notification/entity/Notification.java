package com.app.taskmanagement.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String eventType;

	@Column(nullable = false)
	private String recipientEmail;

	private String recipientName;

	@Column(nullable = false)
	private String subject;

	@Column(columnDefinition = "TEXT")
	private String message;

	private Long entityId;

	private String entityType;

	private String workspaceId;

	private String triggeredBy;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private NotificationStatus status;

	private String errorMessage;

	@Column(nullable = false)
	private Boolean isRead;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private LocalDateTime sentAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		if (status == null) {
			status = NotificationStatus.PENDING;
		}
		if (isRead == null) {
			isRead = false;
		}
	}

	public enum NotificationStatus {
		PENDING, SENT, FAILED
	}
}