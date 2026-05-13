package com.app.taskmanagement.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

	private String eventType; // CARD_ASSIGNED, CARD_UPDATED, COMMENT_ADDED, MEMBER_ADDED, etc.
	private String recipientEmail;
	private String recipientName;
	private Integer recipientUserId; // NEW
	private String subject;
	private String message;
	private Long entityId; // Card ID, Comment ID, etc.
	private String entityType; // CARD, COMMENT, WORKSPACE, etc.
	private String workspaceId;
	private String triggeredBy; // User name who triggered the event
	private Integer triggeredByUserId; // NEW
	private LocalDateTime timestamp;
}