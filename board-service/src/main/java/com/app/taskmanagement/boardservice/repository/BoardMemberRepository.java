package com.app.taskmanagement.boardservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.taskmanagement.boardservice.entity.BoardMember;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for BoardMember. Handles membership checks and role lookups
 * for board access control.
 */
public interface BoardMemberRepository extends JpaRepository<BoardMember, Integer> {

	/**
	 * Find a specific user's membership on a board. Returns Optional because the
	 * user might not be a member.
	 */
	Optional<BoardMember> findByBoardBoardIdAndUserId(int boardId, int userId);

	/**
	 * Get all members of a board — for the members list UI.
	 */
	List<BoardMember> findAllByBoardBoardId(int boardId);

	/**
	 * Check if a user is already a board member. Used before adding someone to
	 * prevent duplicates.
	 */
	boolean existsByBoardBoardIdAndUserId(int boardId, int userId);

	/**
	 * Remove a user from a board.
	 */
	void deleteByBoardBoardIdAndUserId(int boardId, int userId);
}