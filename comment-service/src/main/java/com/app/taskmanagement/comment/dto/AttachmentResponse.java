package com.app.taskmanagement.comment.dto;

import com.app.taskmanagement.comment.entity.Attachment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response payload for an attachment record.
 */
@Getter
@Builder
public class AttachmentResponse {

	private Integer attachmentId;
	private Integer cardId;
	private Integer uploaderUserId;
	private String fileName;
	private String fileUrl;
	private String fileType;
	private Long fileSizeKb;
	private LocalDateTime uploadedAt;

	/**
	 * Whether the requesting user is the one who uploaded this attachment. Controls
	 * visibility of the Delete button in the UI.
	 */
	private boolean isOwner;

	public static AttachmentResponse from(Attachment attachment, int requestUserId) {
		return AttachmentResponse.builder().attachmentId(attachment.getAttachmentId()).cardId(attachment.getCardId())
				.uploaderUserId(attachment.getUploaderUserId()).fileName(attachment.getFileName())
				.fileUrl(attachment.getFileUrl()).fileType(attachment.getFileType())
				.fileSizeKb(attachment.getFileSizeKb()).uploadedAt(attachment.getUploadedAt())
				.isOwner(attachment.getUploaderUserId() == requestUserId).build();
	}
}