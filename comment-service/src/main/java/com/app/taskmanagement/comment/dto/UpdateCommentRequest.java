package com.app.taskmanagement.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for editing an existing comment. Only the content can be changed
 * — parentCommentId is immutable.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateCommentRequest {

	@NotBlank(message = "Comment content must not be blank")
	@Size(max = 5000, message = "Comment must not exceed 5000 characters")
	private String content;
}