package com.app.taskmanagement.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for registering a file attachment on a card.
 *
 * The actual file binary is NOT sent here. It is uploaded directly to S3 by the
 * frontend. This endpoint only stores the metadata record so the app knows what
 * files a card has and where to find them.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddAttachmentRequest {

	@NotBlank(message = "File name must not be blank")
	@Size(max = 255, message = "File name must not exceed 255 characters")
	private String fileName;

	/**
	 * Full S3/CDN URL of the uploaded file. Example:
	 * "https://cdn.flowboard.io/attachments/abc123-report.pdf"
	 */
	@NotBlank(message = "File URL must not be blank")
	@Size(max = 1024, message = "File URL must not exceed 1024 characters")
	private String fileUrl;

	/**
	 * MIME type. Examples: "image/png", "application/pdf", "text/plain" Used by the
	 * frontend to render previews vs. download links.
	 */
	@NotBlank(message = "File type must not be blank")
	private String fileType;

	/**
	 * File size in kilobytes. Must be a positive number. Displayed as "2.4 MB" or
	 * "340 KB" in the UI.
	 */
	@NotNull(message = "File size must be provided")
	@Positive(message = "File size must be positive")
	private Long fileSizeKb;
}