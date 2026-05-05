package com.app.taskmanagement.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request body for adding a new item to an existing checklist.
 *
 * Example JSON: { "text": "Write unit tests" } { "text": "Deploy to staging",
 * "assigneeUserId": 5, "dueDate": "2026-06-01" }
 *
 * assigneeUserId and dueDate are optional — most items are added without them.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddChecklistItemRequest {

	@NotBlank(message = "Item text must not be blank")
	@Size(max = 500, message = "Item text must not exceed 500 characters")
	private String text;

	/**
	 * Optional: the userId of a team member responsible for this specific item. If
	 * null, the card's overall assignee is assumed to be responsible.
	 */
	private Integer assigneeUserId;

	/**
	 * Optional: when this specific item needs to be completed. LocalDate = date
	 * only, no time (e.g. 2026-06-01).
	 */
	private LocalDate dueDate;
}