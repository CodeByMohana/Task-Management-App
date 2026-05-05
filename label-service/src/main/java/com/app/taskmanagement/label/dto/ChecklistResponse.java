package com.app.taskmanagement.label.dto;

import com.app.taskmanagement.label.entity.Checklist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A full checklist with all its items and a computed progress summary.
 *
 * Example JSON returned: { "checklistId": 1, "cardId": 10, "title": "Backend
 * tasks", "position": 1, "items": [ { "text": "Write tests", "completed": true
 * }, { "text": "Deploy", "completed": false } ], "completedCount": 1,
 * "totalCount": 2, "progressPercent": 50 }
 *
 * The progress fields power the card's progress bar in the UI.
 */
@Getter
@Builder
public class ChecklistResponse {

	private Integer checklistId;
	private Integer cardId;
	private String title;
	private Integer position;
	private LocalDateTime createdAt;

	// All items inside this checklist, sorted by position (top to bottom)
	private List<ChecklistItemResponse> items;

	// How many items are ticked as done
	private int completedCount;

	// Total number of items (done + not done)
	private int totalCount;

	/**
	 * Percentage of items completed, rounded to the nearest whole number. Example:
	 * 3 done out of 5 total = 60% Used to render the progress bar: [====------] 60%
	 */
	private int progressPercent;

	/**
	 * Converts a Checklist entity into a ChecklistResponse DTO.
	 *
	 * Progress is calculated here, in the DTO layer, because: - It's a view concern
	 * (how to display the data), not a storage concern. - The service layer doesn't
	 * need to know about percentages. - The items are already sorted
	 * by @OrderBy("position ASC") on the entity.
	 */
	public static ChecklistResponse from(Checklist checklist) {
		// Convert each ChecklistItem entity to its response DTO
		List<ChecklistItemResponse> itemResponses = checklist.getItems().stream().map(ChecklistItemResponse::from)
				.toList();

		int total = itemResponses.size();

		// Count completed items — stream().filter() iterates the list and keeps only
		// matches
		long completed = itemResponses.stream().filter(ChecklistItemResponse::isCompleted).count();

		// Avoid dividing by zero: if no items exist, progress is 0%
		int percent = total > 0 ? (int) ((completed * 100) / total) : 0;

		return ChecklistResponse.builder().checklistId(checklist.getChecklistId()).cardId(checklist.getCardId())
				.title(checklist.getTitle()).position(checklist.getPosition()).createdAt(checklist.getCreatedAt())
				.items(itemResponses).completedCount((int) completed).totalCount(total).progressPercent(percent)
				.build();
	}
}