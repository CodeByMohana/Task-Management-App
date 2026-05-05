package com.app.taskmanagement.comment.repository;

import com.app.taskmanagement.comment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for Attachment entities.
 */
public interface AttachmentRepository extends JpaRepository<Attachment, Integer> {

	/**
	 * Get all attachments for a card, newest first. Most recent uploads appear at
	 * the top of the attachment list in the UI.
	 */
	List<Attachment> findAllByCardIdOrderByUploadedAtDesc(int cardId);

	/**
	 * Find a specific attachment that belongs to a specific card. The cardId check
	 * prevents accessing attachments from other cards by guessing attachment IDs.
	 */
	Optional<Attachment> findByAttachmentIdAndCardId(int attachmentId, int cardId);

	/**
	 * Count how many attachments a card has. Used for the attachment badge count
	 * shown on card tiles.
	 */
	int countByCardId(int cardId);
}