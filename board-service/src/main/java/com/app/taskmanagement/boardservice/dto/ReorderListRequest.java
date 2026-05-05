package com.app.taskmanagement.boardservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for moving a list to a new position (drag-and-drop).
 *
 * Example: user drags "In Progress" (position 3) to position 1 → send {
 * "newPosition": 1 } → service shifts other lists to make room
 */
@Data
public class ReorderListRequest {

	@NotNull(message = "New position is required")
	@Min(value = 1, message = "Position must be at least 1")
	private Integer newPosition;
}