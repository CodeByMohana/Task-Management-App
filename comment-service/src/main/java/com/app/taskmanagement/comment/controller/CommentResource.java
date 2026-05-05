package com.app.taskmanagement.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.app.taskmanagement.comment.dto.*;
import com.app.taskmanagement.comment.service.CommentService;

import java.util.List;

/**
 * REST controller for comment and attachment operations.
 *
 * URL structure: Comments → /api/comments/cards/{cardId}/comments Replies →
 * /api/comments/cards/{cardId}/comments/{commentId}/replies Attachments →
 * /api/comments/cards/{cardId}/attachments
 *
 * The /api/comments prefix is used as the gateway route discriminator so all
 * traffic to this service routes under a single path prefix.
 *
 * @AuthenticationPrincipal Integer userId Spring resolves this from the
 *                          SecurityContext principal set by
 *                          JwtAuthenticationFilter. The userId is the
 *                          authenticated user's ID.
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "Comments & Attachments", description = "Card comment threading and file attachment management")
public class CommentResource {

	private final CommentService commentService;

	// =========================================================================
	// COMMENT ENDPOINTS
	// =========================================================================

	/**
	 * POST /api/comments/cards/{cardId}/comments
	 *
	 * Post a new top-level comment or a reply on a card.
	 *
	 * Body examples: Top-level: { "content": "Looks good to me!" } Reply: {
	 * "content": "Agreed!", "parentCommentId": 7 }
	 *
	 * Returns 201 Created with the saved comment.
	 */
	@PostMapping("/cards/{cardId}/comments")
	@Operation(summary = "Add a comment or reply to a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CommentResponse> addComment(@PathVariable int cardId,
			@Valid @RequestBody AddCommentRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(cardId, request, userId));
	}

	/**
	 * GET /api/comments/cards/{cardId}/comments
	 *
	 * Fetch all top-level comments for a card, each including their replies.
	 * Deleted comments appear with blank content (frontend shows "[deleted]").
	 * Ordered oldest-first so the conversation reads top-to-bottom.
	 */
	@GetMapping("/cards/{cardId}/comments")
	@Operation(summary = "Get all comments for a card (with replies)", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<CommentResponse>> getComments(@PathVariable int cardId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(commentService.getCommentsByCard(cardId, userId));
	}

	/**
	 * GET /api/comments/cards/{cardId}/comments/{commentId}
	 *
	 * Fetch a single comment by ID, including its replies. Useful when the frontend
	 * needs to refresh a specific comment after editing.
	 */
	@GetMapping("/cards/{cardId}/comments/{commentId}")
	@Operation(summary = "Get a single comment with its replies", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CommentResponse> getComment(@PathVariable int cardId, @PathVariable int commentId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(commentService.getCommentById(commentId, userId));
	}

	/**
	 * GET /api/comments/cards/{cardId}/comments/{commentId}/replies
	 *
	 * Fetch only the replies to a specific comment. Used for lazy-loading a comment
	 * thread on demand.
	 */
	@GetMapping("/cards/{cardId}/comments/{commentId}/replies")
	@Operation(summary = "Get replies to a comment", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<CommentResponse>> getReplies(@PathVariable int cardId, @PathVariable int commentId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(commentService.getReplies(commentId, userId));
	}

	/**
	 * PUT /api/comments/cards/{cardId}/comments/{commentId}
	 *
	 * Edit a comment's content. Only the author can edit their own comment. Deleted
	 * comments cannot be edited.
	 *
	 * Returns the updated comment (without replies for efficiency).
	 */
	@PutMapping("/cards/{cardId}/comments/{commentId}")
	@Operation(summary = "Edit a comment (author only)", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CommentResponse> updateComment(@PathVariable int cardId, @PathVariable int commentId,
			@Valid @RequestBody UpdateCommentRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(commentService.updateComment(commentId, request, userId));
	}

	/**
	 * DELETE /api/comments/cards/{cardId}/comments/{commentId}
	 *
	 * Soft-delete a comment. Only the author can delete their own comment. The
	 * comment record is retained with isDeleted=true to preserve reply thread
	 * context. Content is blanked — frontend shows "[deleted]".
	 *
	 * Returns 204 No Content on success.
	 */
	@DeleteMapping("/cards/{cardId}/comments/{commentId}")
	@Operation(summary = "Delete a comment (soft-delete, author only)", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deleteComment(@PathVariable int cardId, @PathVariable int commentId,
			@AuthenticationPrincipal Integer userId) {

		commentService.deleteComment(commentId, userId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * GET /api/comments/cards/{cardId}/comments/count
	 *
	 * Returns the count of active (non-deleted) comments on a card. Used to display
	 * the comment badge on card tiles in board view.
	 *
	 * Example response: 5
	 */
	@GetMapping("/cards/{cardId}/comments/count")
	@Operation(summary = "Count active comments on a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Integer> getCommentCount(@PathVariable int cardId) {
		return ResponseEntity.ok(commentService.getCommentCount(cardId));
	}

	// =========================================================================
	// ATTACHMENT ENDPOINTS
	// =========================================================================

	/**
	 * POST /api/comments/cards/{cardId}/attachments
	 *
	 * Register a file attachment metadata record on a card. The file must already
	 * be uploaded to S3 before calling this endpoint.
	 *
	 * Body: { "fileName": "report.pdf", "fileUrl": "https://...", "fileType":
	 * "application/pdf", "fileSizeKb": 204 }
	 *
	 * Returns 201 Created with the saved attachment metadata.
	 */
	@PostMapping("/cards/{cardId}/attachments")
	@Operation(summary = "Add a file attachment to a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<AttachmentResponse> addAttachment(@PathVariable int cardId,
			@Valid @RequestBody AddAttachmentRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addAttachment(cardId, request, userId));
	}

	/**
	 * GET /api/comments/cards/{cardId}/attachments
	 *
	 * Get all attachment metadata for a card, newest first. The fileUrl in each
	 * record links to the actual file in S3/CDN.
	 */
	@GetMapping("/cards/{cardId}/attachments")
	@Operation(summary = "Get all attachments for a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<AttachmentResponse>> getAttachments(@PathVariable int cardId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(commentService.getAttachments(cardId, userId));
	}

	/**
	 * DELETE /api/comments/cards/{cardId}/attachments/{attachmentId}
	 *
	 * Permanently delete an attachment metadata record. Only the uploader can
	 * delete their own attachment. Note: S3 object deletion must be handled
	 * separately.
	 *
	 * Returns 204 No Content on success.
	 */
	@DeleteMapping("/cards/{cardId}/attachments/{attachmentId}")
	@Operation(summary = "Delete an attachment (uploader only)", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deleteAttachment(@PathVariable int cardId, @PathVariable int attachmentId,
			@AuthenticationPrincipal Integer userId) {

		commentService.deleteAttachment(cardId, attachmentId, userId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * GET /api/comments/cards/{cardId}/attachments/count
	 *
	 * Returns the total number of attachments on a card. Used to display the
	 * attachment badge on card tiles in board view.
	 */
	@GetMapping("/cards/{cardId}/attachments/count")
	@Operation(summary = "Count attachments on a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Integer> getAttachmentCount(@PathVariable int cardId) {
		return ResponseEntity.ok(commentService.getAttachmentCount(cardId));
	}
}