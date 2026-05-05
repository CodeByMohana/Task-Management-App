package com.app.taskmanagement.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.app.taskmanagement.comment.dto.*;
import com.app.taskmanagement.comment.entity.Attachment;
import com.app.taskmanagement.comment.entity.Comment;
import com.app.taskmanagement.comment.exception.*;
import com.app.taskmanagement.comment.repository.AttachmentRepository;
import com.app.taskmanagement.comment.repository.CommentRepository;
import com.app.taskmanagement.comment.messaging.NotificationPublisher;

import java.util.List;

/**
 * Implementation of all comment and attachment business logic.
 *
 * Key design decisions documented inline:
 *
 * THREADING: We support exactly two levels: top-level comment → reply. Replies
 * cannot themselves have replies. This matches Trello behaviour and prevents
 * infinite nesting in the UI.
 *
 * SOFT-DELETE: Comments are never hard-deleted. When a user deletes a comment
 * we set isDeleted=true and blank the content. This preserves reply thread
 * context — a reply to a deleted comment still makes sense visually as
 * "[deleted] → reply".
 *
 * OWNERSHIP ENFORCEMENT: Edit and delete are restricted to the comment's own
 * author. We intentionally do NOT give board admins the ability to edit/delete
 * others' comments — only soft-delete is permitted, which keeps the audit trail
 * intact. Platform admins could be granted this via a role check in future.
 *
 * ATTACHMENT OWNERSHIP: Only the uploader can delete an attachment. This is a
 * metadata-only delete — the S3 object must be cleaned up separately (future:
 * S3 lifecycle policy or a storage service event).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

	private final CommentRepository commentRepository;
	private final AttachmentRepository attachmentRepository;
	private final NotificationPublisher notificationPublisher;

	// =========================================================================
	// COMMENT OPERATIONS
	// =========================================================================

	@Override
	public CommentResponse addComment(int cardId, AddCommentRequest request, int authorUserId) {

		/*
		 * If this is a reply, validate the parent: 1. Parent must exist. 2. Parent must
		 * belong to the same card — prevents cross-card threading. 3. Parent must not
		 * itself be a reply (no nesting beyond one level).
		 */
		if (request.getParentCommentId() != null) {
			Comment parent = commentRepository.findById(request.getParentCommentId()).orElseThrow(
					() -> new ResourceNotFoundException("Parent comment not found: " + request.getParentCommentId()));

			if (parent.getCardId() != cardId) {
				throw new BadRequestException("Parent comment does not belong to this card");
			}

			if (parent.getParentCommentId() != null) {
				throw new BadRequestException("Cannot reply to a reply. Only one level of threading is supported");
			}
		}

		Comment comment = Comment.builder().cardId(cardId).authorUserId(authorUserId).content(request.getContent())
				.parentCommentId(request.getParentCommentId()).build();

		Comment savedComment = commentRepository.save(comment);
		
		// If it's a reply to someone, notify them
		if (request.getParentCommentId() != null) {
			Comment parent = commentRepository.findById(request.getParentCommentId()).orElse(null);
			if (parent != null && parent.getAuthorUserId() != authorUserId) {
				notificationPublisher.notifyCommentAdded(
					parent.getAuthorUserId(),
					"Card #" + cardId, // Ideally we'd have the real card title, but comment-service doesn't store it
					request.getContent(),
					authorUserId,
					(long) savedComment.getCommentId()
				);
			}
		}

		return CommentResponse.from(savedComment, authorUserId, false);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CommentResponse> getCommentsByCard(int cardId, int requestingUserId) {
		/*
		 * Fetch only top-level comments (parentCommentId IS NULL). For each, include
		 * their replies by passing includeReplies=true.
		 *
		 * The @OneToMany on Comment.replies is LAZY — it loads only when we access
		 * comment.getReplies() inside CommentResponse.from(). This is acceptable here
		 * because we need replies for every comment in the thread view. For a card with
		 * 10 comments × 3 replies each, this is 10 extra queries — tolerable. Future:
		 * JOIN FETCH if needed.
		 */
		return commentRepository.findAllByCardIdAndParentCommentIdIsNullOrderByCreatedAtAsc(cardId).stream()
				.map(comment -> CommentResponse.from(comment, requestingUserId, true)).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public CommentResponse getCommentById(int commentId, int requestingUserId) {
		Comment comment = findCommentById(commentId);
		return CommentResponse.from(comment, requestingUserId, true);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CommentResponse> getReplies(int commentId, int requestingUserId) {
		// Ensure the parent comment actually exists before fetching its replies
		findCommentById(commentId);

		return commentRepository.findAllByParentCommentIdOrderByCreatedAtAsc(commentId).stream()
				.map(reply -> CommentResponse.from(reply, requestingUserId, false)).toList();
	}

	@Override
	public CommentResponse updateComment(int commentId, UpdateCommentRequest request, int requestingUserId) {
		Comment comment = findCommentById(commentId);

		// Deleted comments cannot be edited — there is no content to update
		if (comment.isDeleted()) {
			throw new BadRequestException("Cannot edit a deleted comment");
		}

		// Only the author can edit their own comment
		requireCommentOwner(comment, requestingUserId, "edit");

		comment.setContent(request.getContent());
		return CommentResponse.from(commentRepository.save(comment), requestingUserId, false);
	}

	@Override
	public void deleteComment(int commentId, int requestingUserId) {
		Comment comment = findCommentById(commentId);

		if (comment.isDeleted()) {
			throw new BadRequestException("Comment is already deleted");
		}

		// Only the author can delete their own comment
		requireCommentOwner(comment, requestingUserId, "delete");

		/*
		 * Soft-delete: blank the content and mark as deleted. We do NOT remove the
		 * entity — replies would lose their parent reference.
		 *
		 * The frontend shows "[deleted]" when deleted=true and content is "".
		 */
		comment.setDeleted(true);
		comment.setContent("");
		commentRepository.save(comment);
	}

	@Override
	@Transactional(readOnly = true)
	public int getCommentCount(int cardId) {
		return commentRepository.countActiveByCardId(cardId);
	}

	// =========================================================================
	// ATTACHMENT OPERATIONS
	// =========================================================================

	@Override
	public AttachmentResponse addAttachment(int cardId, AddAttachmentRequest request, int uploaderUserId) {
		/*
		 * We intentionally do NOT validate that the cardId exists in card-service.
		 * Reason: inter-service HTTP calls add latency and a failure point. The gateway
		 * enforces authentication; the calling client is responsible for providing a
		 * valid cardId. If card-service deletes a card in future, orphaned attachment
		 * records can be cleaned up by a background job.
		 *
		 * This is the standard microservice "accept and trust" pattern for
		 * cross-service foreign keys.
		 */
		Attachment attachment = Attachment.builder().cardId(cardId).uploaderUserId(uploaderUserId)
				.fileName(request.getFileName()).fileUrl(request.getFileUrl()).fileType(request.getFileType())
				.fileSizeKb(request.getFileSizeKb()).build();

		return AttachmentResponse.from(attachmentRepository.save(attachment), uploaderUserId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AttachmentResponse> getAttachments(int cardId, int requestingUserId) {
		return attachmentRepository.findAllByCardIdOrderByUploadedAtDesc(cardId).stream()
				.map(a -> AttachmentResponse.from(a, requestingUserId)).toList();
	}

	@Override
	public void deleteAttachment(int cardId, int attachmentId, int requestingUserId) {
		/*
		 * The cardId + attachmentId double-check prevents a user from deleting
		 * attachments on other cards by guessing attachment IDs.
		 */
		Attachment attachment = attachmentRepository.findByAttachmentIdAndCardId(attachmentId, cardId)
				.orElseThrow(() -> new ResourceNotFoundException("Attachment not found on card " + cardId));

		// Only the uploader can delete their own attachment
		if (attachment.getUploaderUserId() != requestingUserId) {
			throw new ForbiddenException("Only the uploader can delete this attachment");
		}

		/*
		 * Hard-delete the metadata record. Note: S3 object deletion is NOT done here.
		 * Options for cleanup: 1. S3 lifecycle expiry policy (simplest, eventual). 2.
		 * Publish a domain event (AttachmentDeleted) consumed by a storage service. 3.
		 * Call an S3 client directly here (couples this service to S3 SDK). Current
		 * choice: option 1 — acceptable for this stage.
		 */
		attachmentRepository.delete(attachment);
	}

	@Override
	@Transactional(readOnly = true)
	public int getAttachmentCount(int cardId) {
		return attachmentRepository.countByCardId(cardId);
	}

	// =========================================================================
	// PRIVATE HELPERS
	// =========================================================================

	/**
	 * Fetch a comment or throw 404. Centralised to avoid repeating orElseThrow.
	 */
	private Comment findCommentById(int commentId) {
		return commentRepository.findById(commentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
	}

	/**
	 * Throw 403 if the requesting user is not the comment's author.
	 *
	 * @param action short verb for the error message ("edit" / "delete")
	 */
	private void requireCommentOwner(Comment comment, int requestingUserId, String action) {
		if (comment.getAuthorUserId() != requestingUserId) {
			throw new ForbiddenException("You can only " + action + " your own comments");
		}
	}
}