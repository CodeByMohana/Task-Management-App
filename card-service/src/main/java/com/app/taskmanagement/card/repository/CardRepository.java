package com.app.taskmanagement.card.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.taskmanagement.card.entity.Card;

import java.time.LocalDate;
import java.util.List;

/**
 * Data access layer for Card.
 *
 * Spring Data JPA generates SQL automatically from method names. Custom JPQL
 * queries handle complex operations like position shifting.
 */
public interface CardRepository extends JpaRepository<Card, Integer> {

	/**
	 * Get all active (non-archived) cards in a list, ordered top-to-bottom. This is
	 * what populates the cards in each column on the board view.
	 */
	List<Card> findAllByListIdAndArchivedFalseOrderByPositionAsc(int listId);

	/**
	 * Get all archived cards in a list — for the archive panel.
	 */
	List<Card> findAllByListIdAndArchivedTrueOrderByPositionAsc(int listId);

	/**
	 * Get all active cards on a board — used for board-level analytics.
	 */
	List<Card> findAllByBoardIdAndArchivedFalse(int boardId);

	/**
	 * Get all cards assigned to a specific user across all boards. Used for a "My
	 * Cards" dashboard view.
	 */
	List<Card> findAllByAssigneeUserIdAndArchivedFalse(int assigneeUserId);

	/**
	 * Find all cards due on or before a specific date. Used by the scheduler to
	 * send due-date reminder notifications. Only checks active (non-archived,
	 * non-done) cards.
	 */
	@Query("""
			SELECT c FROM Card c
			WHERE c.dueDate <= :date
			AND c.archived = false
			AND c.status != 'DONE'
			""")
	List<Card> findOverdueOrDueSoon(@Param("date") LocalDate date);

	/**
	 * Search cards by title within a specific board. ContainingIgnoreCase = SQL
	 * LIKE '%title%' (case-insensitive).
	 */
	List<Card> findByBoardIdAndTitleContainingIgnoreCaseAndArchivedFalse(int boardId, String title);

	/**
	 * Search cards by assignee within a board.
	 */
	List<Card> findByBoardIdAndAssigneeUserIdAndArchivedFalse(int boardId, int assigneeUserId);

	/**
	 * Find the highest position value in a list. Used when adding a new card — it
	 * goes after the last card. Returns 0 if no cards exist yet (first card gets
	 * position 1).
	 */
	@Query("""
			SELECT COALESCE(MAX(c.position), 0)
			FROM Card c
			WHERE c.listId = :listId
			AND c.archived = false
			""")
	int findMaxPositionByListId(@Param("listId") int listId);

	/**
	 * Shift card positions DOWN when a card is moved away. Example: card at
	 * position 3 moved out → cards at positions 4,5,6 become 3,4,5
	 */
	@Modifying
	@Query("""
			UPDATE Card c
			SET c.position = c.position - 1
			WHERE c.listId = :listId
			AND c.position > :fromPosition
			AND c.cardId != :excludeCardId
			AND c.archived = false
			""")
	void shiftPositionsDown(@Param("listId") int listId, @Param("fromPosition") int fromPosition,
			@Param("excludeCardId") int excludeCardId);

	/**
	 * Shift card positions UP when a card is inserted at a position. Example: card
	 * inserted at position 2 → cards at positions 2,3,4 become 3,4,5
	 */
	@Modifying
	@Query("""
			UPDATE Card c
			SET c.position = c.position + 1
			WHERE c.listId = :listId
			AND c.position >= :fromPosition
			AND c.cardId != :excludeCardId
			AND c.archived = false
			""")
	void shiftPositionsUp(@Param("listId") int listId, @Param("fromPosition") int fromPosition,
			@Param("excludeCardId") int excludeCardId);

	/**
	 * Count active cards in a list — for list-level analytics.
	 */
	int countByListIdAndArchivedFalse(int listId);

	/**
	 * Count active cards on a board — for board-level analytics.
	 */
	int countByBoardIdAndArchivedFalse(int boardId);
}
