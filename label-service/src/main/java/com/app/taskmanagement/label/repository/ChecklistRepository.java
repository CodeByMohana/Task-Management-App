package com.app.taskmanagement.label.repository;

import com.app.taskmanagement.label.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Database access for Checklist entities.
 */
public interface ChecklistRepository extends JpaRepository<Checklist, Integer> {

	/**
	 * Get all checklists for a card, ordered by their display position (top to
	 * bottom). This is called every time the card detail modal is opened.
	 *
	 * Example: card 10 has 2 checklists → returns [Checklist "Backend" pos=1,
	 * Checklist "Frontend" pos=2]
	 */
	List<Checklist> findAllByCardIdOrderByPositionAsc(int cardId);

	/**
	 * Find the highest position number among all checklists on a card. Used to
	 * auto-assign the next position when creating a new checklist.
	 *
	 * Example: card has checklists at positions 1, 2, 3 → returns 3. New checklist
	 * gets position 4.
	 *
	 * COALESCE means "if the result is null (no checklists yet), return 0 instead".
	 * Without COALESCE, the first checklist would get a null position, which would
	 * crash.
	 */
	@Query("SELECT COALESCE(MAX(c.position), 0) FROM Checklist c WHERE c.cardId = :cardId")
	int findMaxPositionByCardId(@Param("cardId") int cardId);

	/**
	 * Count how many checklists a card has. Not used internally yet, but useful for
	 * card analytics in the future.
	 */
	int countByCardId(int cardId);
}