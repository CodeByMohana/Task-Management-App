package com.app.taskmanagement.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for posting a new comment or reply.
 *
 * For a top-level comment: send only 'content'. For a reply: send 'content' +
 * 'parentCommentId'.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddCommentRequest {

	/**
	 * The comment text. Must not be blank. Max 5000 characters to prevent oversized
	 * payloads while still allowing detailed technical comments.
	 */
	@NotBlank(message = "Comment content must not be blank")
	@Size(max = 5000, message = "Comment must not exceed 5000 characters")
	private String content;

	/**
	 * ID of the parent comment if this is a reply. Null for top-level comments. The
	 * service validates that the parent exists and belongs to the same card.
	 */
	private Integer parentCommentId;
}