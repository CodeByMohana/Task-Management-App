package com.app.taskmanagement.boardservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a List (column) on a Kanban board.
 *
 * Named "BoardList" instead of "List" to avoid conflict with java.util.List.
 *
 * Example lists on a board: Position 1: "Backlog" Position 2: "In Progress"
 * Position 3: "In Review" Position 4: "Done"
 *
 * The 'position' field controls the left-to-right order of columns. When a user
 * drags a list to a new position, we update this integer.
 *
 * Archived lists are hidden from normal view but not deleted, so they can be
 * restored later.
 */
@Entity
@Table(name = "board_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardList {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer listId;

	/**
	 * The board this list belongs to. Many lists can belong to one board.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "board_id", nullable = false)
	private Board board;

	/**
	 * Display name of this list/column. Example: "To Do", "In Progress", "Done",
	 * "Backlog"
	 */
	@Column(nullable = false)
	private String name;

	/**
	 * Controls the display order (left to right) of lists on the board. Lower
	 * number = further left. When reordering: update position values of affected
	 * lists.
	 *
	 * Example: dragging list from position 3 to position 1 → old pos 1 becomes 2,
	 * old pos 2 becomes 3, dragged becomes 1
	 */
	@Column(nullable = false)
	private Integer position;

	/**
	 * Soft-delete flag. Archived lists are hidden from the board view but remain in
	 * the DB so they can be restored. True = hidden, False = visible (default).
	 */
	@Column(nullable = false)
	@Builder.Default
	private boolean archived = false;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;
}