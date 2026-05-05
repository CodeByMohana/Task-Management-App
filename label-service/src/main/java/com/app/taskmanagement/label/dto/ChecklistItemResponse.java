package com.app.taskmanagement.label.dto;

import com.app.taskmanagement.label.entity.ChecklistItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single checklist item as returned to the client.
 *
 * Example JSON returned: { "itemId": 1, "text": "Write unit tests",
 * "completed": false, "position": 1, "assigneeUserId": null, "dueDate": null }
 */
@Getter
@Builder
public class ChecklistItemResponse {

	private Integer itemId;
	private String text;

	/**
	 * true = item has a checkmark (done) false = item is not done yet (default) The
	 * frontend shows completed items with a strikethrough style.
	 */
	private boolean completed;

	// Position within the checklist (1 = top, 2 = next, etc.)
	private Integer position;

	// Optional: the specific person assigned to this item
	private Integer assigneeUserId;

	// Optional: this item's own due date
	private LocalDate dueDate;

	private LocalDateTime createdAt;

	/**
	 * Converts a ChecklistItem entity to this response DTO.
	 */
	public static ChecklistItemResponse from(ChecklistItem item) {
		return ChecklistItemResponse.builder().itemId(item.getItemId()).text(item.getText())
				.completed(item.isCompleted()).position(item.getPosition()).assigneeUserId(item.getAssigneeUserId())
				.dueDate(item.getDueDate()).createdAt(item.getCreatedAt()).build();
	}
}