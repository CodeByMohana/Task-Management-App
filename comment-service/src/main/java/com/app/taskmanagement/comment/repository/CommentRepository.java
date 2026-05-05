package com.app.taskmanagement.comment.repository;

import com.app.taskmanagement.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Data access for Comment entities.
 *
 * Query strategy: We fetch top-level comments (parentCommentId IS NULL) ordered
 * by createdAt ascending so the conversation reads chronologically
 * top-to-bottom. Replies are fetched separately per comment to avoid loading
 * the entire thread at once.
 */
public interface CommentRepository extends JpaRepository<Comment, Integer> {

	/**
	 * Get all top-level (non-reply) comments for a card, oldest first. Deleted
	 * comments are included — the service layer shows "[deleted]" placeholder text
	 * instead of filtering them out, to preserve reply context.
	 *
	 * Example: card 42 has 3 comments → returns all 3 in order of posting.
	 */
	List<Comment> findAllByCardIdAndParentCommentIdIsNullOrderByCreatedAtAsc(int cardId);

	/**
	 * Get all direct replies to a specific parent comment, oldest first. Used when
	 * expanding a comment thread.
	 *
	 * Example: comment 7 has 2 replies → returns both in posting order.
	 */
	List<Comment> findAllByParentCommentIdOrderByCreatedAtAsc(int parentCommentId);

	/**
	 * Count active (non-deleted) comments on a card. Used for the comment badge
	 * count shown on card tiles in board view.
	 *
	 * Returns only non-deleted, regardless of whether they're top-level or replies,
	 * so the badge reflects actual visible content.
	 */
	@Query("""
			SELECT COUNT(c)
			FROM Comment c
			WHERE c.cardId = :cardId
			AND c.deleted = false
			""")
	int countActiveByCardId(@Param("cardId") int cardId);

	/**
	 * Check if a specific user has any non-deleted comments on a card. Used to
	 * determine if a user is already "watching" a card via commenting.
	 */
	boolean existsByCardIdAndAuthorUserIdAndDeletedFalse(int cardId, int authorUserId);
}