package com.app.taskmanagement.comment.dto;

import com.app.taskmanagement.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload for a comment.
 *
 * The 'replies' field is populated only when fetching a single comment's
 * thread. For the card-level comment list, replies are empty to keep the
 * payload light (client fetches replies on demand when expanding a thread).
 *
 * 'deleted' flag is exposed so the frontend can render "[deleted]" placeholder
 * instead of the blank content.
 */
@Getter
@Builder
public class CommentResponse {

	private Integer commentId;
	private Integer cardId;
	private Integer authorUserId;

	/**
	 * Blank string when deleted = true. Frontend shows "[deleted]" placeholder.
	 */
	private String content;

	private Integer parentCommentId;

	/**
	 * True if this comment was soft-deleted. Frontend should show "[deleted]" and
	 * hide edit/delete controls.
	 */
	private boolean deleted;

	/**
	 * Whether the currently authenticated user authored this comment. Used by the
	 * frontend to show/hide the Edit and Delete buttons. Computed at response-build
	 * time using the requesting userId.
	 */
	private boolean isOwner;

	/**
	 * Direct replies to this comment. Empty list for card-level list responses.
	 * Populated when fetching a specific comment's replies.
	 */
	private List<CommentResponse> replies;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	/**
	 * Converts a Comment entity to a response DTO.
	 *
	 * @param comment        the entity to convert
	 * @param requestUserId  the ID of the user making the request (for isOwner)
	 * @param includeReplies whether to populate the replies list
	 */
	public static CommentResponse from(Comment comment, int requestUserId, boolean includeReplies) {
		List<CommentResponse> replyResponses = List.of();

		if (includeReplies && comment.getReplies() != null) {
			replyResponses = comment.getReplies().stream()
					.map(reply -> CommentResponse.from(reply, requestUserId, false)) // replies don't nest further
					.toList();
		}

		return CommentResponse.builder().commentId(comment.getCommentId()).cardId(comment.getCardId())
				.authorUserId(comment.getAuthorUserId())
				// Show blank content for deleted comments — frontend renders placeholder
				.content(comment.isDeleted() ? "" : comment.getContent()).parentCommentId(comment.getParentCommentId())
				.deleted(comment.isDeleted()).isOwner(comment.getAuthorUserId() == requestUserId)
				.replies(replyResponses).createdAt(comment.getCreatedAt()).updatedAt(comment.getUpdatedAt()).build();
	}
}