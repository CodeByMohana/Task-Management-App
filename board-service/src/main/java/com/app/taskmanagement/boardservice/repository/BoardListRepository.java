package com.app.taskmanagement.boardservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.taskmanagement.boardservice.entity.BoardList;

import java.util.List;

/**
 * Data access layer for BoardList (columns on a board).
 */
public interface BoardListRepository extends JpaRepository<BoardList, Integer> {

	/**
	 * Get all active (non-archived) lists for a board, ordered by position. This is
	 * what populates the board view — left to right columns.
	 */
	List<BoardList> findAllByBoardBoardIdAndArchivedFalseOrderByPositionAsc(int boardId);

	/**
	 * Get all archived lists for a board — for the archive panel.
	 */
	List<BoardList> findAllByBoardBoardIdAndArchivedTrueOrderByPositionAsc(int boardId);

	/**
	 * Find the highest position value on a board. Used when adding a new list — it
	 * goes after the last existing one.
	 *
	 * Example: board has lists at positions 1, 2, 3 → new list gets position 4
	 *
	 * Returns 0 if no lists exist yet (so first list gets position 1).
	 */
	@Query("""
			SELECT COALESCE(MAX(l.position), 0)
			FROM BoardList l
			WHERE l.board.boardId = :boardId
			""")
	int findMaxPositionByBoardId(@Param("boardId") int boardId);

	/**
	 * Shift positions of lists to make room when reordering.
	 *
	 * Example: user drags list to position 2 → all lists currently at position >= 2
	 * shift up by 1 → then we set the dragged list to position 2
	 */
	@Modifying
	@Query("""
			UPDATE BoardList l
			SET l.position = l.position + 1
			WHERE l.board.boardId = :boardId
			AND l.position >= :fromPosition
			AND l.listId != :excludeListId
			""")
	void shiftPositionsUp(@Param("boardId") int boardId, @Param("fromPosition") int fromPosition,
			@Param("excludeListId") int excludeListId);

	/**
	 * Count how many active lists are on a board. Useful for analytics.
	 */
	int countByBoardBoardIdAndArchivedFalse(int boardId);
}