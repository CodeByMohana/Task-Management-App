package com.app.taskmanagement.boardservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Analytics data for a board — returned by the analytics endpoint.
 *
 * Shows board owners a snapshot of progress: - How many cards are in each list
 * - How many cards are overdue - Overall completion rate
 */
@Data
@Builder
public class BoardAnalyticsResponse {

	private Integer boardId;
	private String boardName;

	/**
	 * Map of listName → card count. Example: { "To Do": 5, "In Progress": 3,
	 * "Done": 12 }
	 */
	private Map<String, Integer> cardCountPerList;

	/**
	 * Total number of active (non-archived) cards on the board.
	 */
	private int totalCards;

	/**
	 * Number of active lists (columns) on the board.
	 */
	private int totalLists;

	/**
	 * Number of board members.
	 */
	private int totalMembers;
}