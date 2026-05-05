package com.app.taskmanagement.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

import com.app.taskmanagement.card.entity.Card;

/**
 * Request body sent when creating a new card.
 *
 * listId, boardId, workspaceId are required so card-service knows exactly where
 * the card belongs without calling other services.
 */
@Data
public class CreateCardRequest {

	@NotNull(message = "List ID is required")
	private Integer listId;

	@NotNull(message = "Board ID is required")
	private Integer boardId;

	@NotNull(message = "Workspace ID is required")
	private Integer workspaceId;

	@NotBlank(message = "Card title is required")
	@Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
	private String title;

	private String description;

	/** Defaults to MEDIUM if not specified */
	private Card.Priority priority = Card.Priority.MEDIUM;

	/** The userId to assign this card to (optional) */
	private Integer assigneeUserId;

	private LocalDate dueDate;

	private LocalDate startDate;

	private String coverColor;
	
	private String boardName;
	
	private String workspaceName;
}