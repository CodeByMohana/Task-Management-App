package com.app.taskmanagement.card.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for moving a card to a different list or position.
 *
 * Used for both: 1. Moving to a different list (drag card between columns) →
 * send new listId + position
 *
 * 2. Reordering within the same list (drag card up/down) → send same listId +
 * new position
 */
@Data
public class MoveCardRequest {

	/**
	 * The list (column) to move the card to. Can be the same listId (reordering) or
	 * a different one (moving between lists).
	 */
	@NotNull(message = "Target list ID is required")
	private Integer targetListId;

	/**
	 * The position in the target list where the card should be placed. Position 1 =
	 * top of the list.
	 */
	@NotNull(message = "New position is required")
	@Min(value = 1, message = "Position must be at least 1")
	private Integer newPosition;
}