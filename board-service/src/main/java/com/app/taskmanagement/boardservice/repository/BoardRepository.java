package com.app.taskmanagement.boardservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.taskmanagement.boardservice.entity.Board;

import java.util.List;

/**
 * Data access layer for Board.
 *
 * Spring Data JPA generates the SQL automatically from method names. JPQL
 * queries use entity class names and field names (not table/column names).
 */
public interface BoardRepository extends JpaRepository<Board, Integer> {

	/**
	 * Get all boards in a workspace (including closed boards).
	 *
	 * A user can see a board if ANY of these are true: 1. Board is PUBLIC 2. Board
	 * is WORKSPACE visibility (user is in the workspace) 3. Board is PRIVATE but
	 * user is a direct board member
	 *
	 * Closed boards are included so users can reopen them from the workspace page.
	 */
	@Query("""
			SELECT b FROM Board b
			WHERE b.workspaceId = :workspaceId
			""")
	List<Board> findAllByWorkspaceId(@Param("workspaceId") int workspaceId);

	/**
	 * Find all boards where the user is a direct member. Used to find PRIVATE
	 * boards the user has access to.
	 */
	@Query("""
			SELECT b FROM Board b
			JOIN b.members m
			WHERE m.userId = :userId
			AND b.closed = false
			""")
	List<Board> findAllByMemberUserId(@Param("userId") int userId);

	/**
	 * Find all PUBLIC boards in a workspace — for guest/unauthenticated viewing.
	 */
	List<Board> findAllByWorkspaceIdAndVisibilityAndClosedFalse(int workspaceId, Board.Visibility visibility);

	/**
	 * Find all boards created by a specific user across all workspaces.
	 */
	List<Board> findAllByCreatedByUserIdAndClosedFalse(int userId);
}