package com.app.taskmanagement.comment.service;

import com.app.taskmanagement.comment.dto.*;

import java.util.List;

/**
 * Business contract for comment and attachment operations.
 *
 * Separation of interface from impl allows: - Swapping implementations (e.g.
 * mock for tests) without changing controllers. - Cleaner dependency injection
 * — controllers depend on the abstraction. - Consistent with the pattern used
 * across all other FlowBoard services.
 */
public interface CommentService {

	// ── Comments ─────────────────────────────────────────────────────────────

	/**
	 * Post a new comment or reply on a card. If request.parentCommentId is set,
	 * validates the parent exists on the same card.
	 */
	CommentResponse addComment(int cardId, AddCommentRequest request, int authorUserId);

	/**
	 * Get all top-level comments for a card, each with their replies included.
	 * Deleted comments are returned with blank content — the frontend shows
	 * "[deleted]".
	 */
	List<CommentResponse> getCommentsByCard(int cardId, int requestingUserId);

	/**
	 * Get a single comment by ID, including its replies.
	 */
	CommentResponse getCommentById(int commentId, int requestingUserId);

	/**
	 * Get all replies to a specific comment.
	 */
	List<CommentResponse> getReplies(int commentId, int requestingUserId);

	/**
	 * Edit a comment's content. Only the author can edit their own comment. Deleted
	 * comments cannot be edited.
	 */
	CommentResponse updateComment(int commentId, UpdateCommentRequest request, int requestingUserId);

	/**
	 * Soft-delete a comment. Only the author can delete their own comment. Sets
	 * isDeleted=true and blanks the content — does NOT remove from DB. This
	 * preserves reply thread context.
	 */
	void deleteComment(int commentId, int requestingUserId);

	/**
	 * Count active (non-deleted) comments on a card. Used for the comment badge on
	 * card tiles in board view.
	 */
	int getCommentCount(int cardId);

	// ── Attachments ───────────────────────────────────────────────────────────

	/**
	 * Register a file attachment metadata record for a card. The actual file must
	 * already be uploaded to S3 before calling this.
	 */
	AttachmentResponse addAttachment(int cardId, AddAttachmentRequest request, int uploaderUserId);

	/**
	 * Get all attachments for a card, newest first.
	 */
	List<AttachmentResponse> getAttachments(int cardId, int requestingUserId);

	/**
	 * Permanently delete an attachment record. Only the uploader can delete their
	 * own attachment. Note: this deletes only the metadata — S3 cleanup is a
	 * separate concern.
	 */
	void deleteAttachment(int cardId, int attachmentId, int requestingUserId);

	/**
	 * Count attachments on a card. Used for the attachment badge on card tiles in
	 * board view.
	 */
	int getAttachmentCount(int cardId);
}