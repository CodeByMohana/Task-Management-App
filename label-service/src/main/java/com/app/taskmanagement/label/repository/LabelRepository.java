package com.app.taskmanagement.label.repository;

import com.app.taskmanagement.label.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Database access for Label entities.
 *
 * JpaRepository gives us free CRUD methods: save(), findById(), findAll(),
 * delete(), count(), etc.
 *
 * We add custom methods below by following Spring's method naming conventions.
 * Spring reads the method name and generates the SQL automatically. Example:
 * findAllByBoardId(42) → SELECT * FROM labels WHERE board_id = 42
 */
public interface LabelRepository extends JpaRepository<Label, Integer> {

	/**
	 * Get all labels that belong to a specific board. This is the main query —
	 * called every time someone opens a board to show which labels are available
	 * for cards on that board.
	 *
	 * Example: boardId=5 → returns ["Bug" (red), "Feature" (green), "Urgent"
	 * (orange)]
	 */
	List<Label> findAllByBoardIdOrderByCreatedAtAsc(int boardId);

	/**
	 * Get a specific label, but ONLY if it belongs to the expected board. The
	 * boardId check prevents users from editing labels on boards they don't own.
	 *
	 * Example: findByLabelIdAndBoardId(3, 5) → only returns label 3 if it's on
	 * board 5.
	 */
	Optional<Label> findByLabelIdAndBoardId(int labelId, int boardId);

	/**
	 * Check if a label with this name already exists on the board. Used to prevent
	 * duplicate label names (e.g. two "Bug" labels on the same board).
	 * Case-insensitive because "BUG" and "bug" would look the same to users.
	 */
	boolean existsByBoardIdAndNameIgnoreCase(int boardId, String name);
}