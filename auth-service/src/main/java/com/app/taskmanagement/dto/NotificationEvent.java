package com.app.taskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

	private String eventType; // CARD_ASSIGNED, OTP_EMAIL, etc.
	private String recipientEmail;
	private String recipientName;
	private Integer recipientUserId;
	private String subject;
	private String message;
	private Long entityId;
	private String entityType;
	private String workspaceId;
	private String triggeredBy;
	private Integer triggeredByUserId;
	private LocalDateTime timestamp;
}
