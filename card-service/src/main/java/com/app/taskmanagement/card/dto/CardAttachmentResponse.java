package com.app.taskmanagement.card.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.app.taskmanagement.card.entity.CardAttachment;

/**
 * Represents one attachment in the card response.
 */
@Data
@Builder
public class CardAttachmentResponse {

	private Integer attachmentId;
	private String fileName;
	private String fileType;
	private Long fileSize;
	private String fileUrl;
	private Integer uploadedByUserId;
	private LocalDateTime uploadedAt;

	public static CardAttachmentResponse from(CardAttachment attachment) {
		return CardAttachmentResponse.builder().attachmentId(attachment.getAttachmentId())
				.fileName(attachment.getFileName()).fileType(attachment.getFileType())
				.fileSize(attachment.getFileSize()).fileUrl(attachment.getFileUrl())
				.uploadedByUserId(attachment.getUploadedByUserId()).uploadedAt(attachment.getUploadedAt()).build();
	}
}