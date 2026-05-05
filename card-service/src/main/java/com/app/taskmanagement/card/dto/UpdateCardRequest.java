package com.app.taskmanagement.card.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

import com.app.taskmanagement.card.entity.Card;

/**
 * Request body for updating an existing card. All fields are optional — only
 * non-null fields will be updated (PATCH-style).
 *
 * This means the client can send just { "title": "New Title" } without needing
 * to resend all other fields.
 */
@Data
public class UpdateCardRequest {

	@Size(min = 1, max = 255)
	private String title;

	private String description;

	private Card.Priority priority;

	private Card.Status status;

	/**
	 * Set to a userId to assign, or 0 to unassign. Using Integer (nullable) — null
	 * means "don't change the assignee".
	 */
	private Integer assigneeUserId;

	private LocalDate dueDate;

	private LocalDate startDate;

	private String coverColor;

	private String boardName;

	private String workspaceName;
}