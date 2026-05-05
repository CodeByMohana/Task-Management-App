package com.app.taskmanagement.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for adding a file attachment to a card.
 *
 * The actual file upload to S3 is handled separately by the frontend. The
 * frontend uploads the file directly to S3 (pre-signed URL), then calls this
 * endpoint to register the metadata in card-service.
 *
 * Flow: 1. Frontend requests a pre-signed S3 upload URL from a storage service
 * 2. Frontend uploads file directly to S3 3. Frontend calls POST
 * /api/cards/{id}/attachments with this metadata 4. card-service saves the
 * metadata record
 */
@Data
public class AddAttachmentRequest {

	@NotBlank(message = "File name is required")
	private String fileName;

	@NotBlank(message = "File type is required")
	private String fileType;

	/**
	 * File size in bytes. Used for displaying human-readable size: "204 KB", "1.2
	 * MB"
	 */
	@NotNull(message = "File size is required")
	private Long fileSize;

	/**
	 * The S3 URL or storage URL where the file was uploaded. Example:
	 * "https://flowboard-bucket.s3.amazonaws.com/cards/5/mockup.png"
	 */
	@NotBlank(message = "File URL is required")
	private String fileUrl;
}