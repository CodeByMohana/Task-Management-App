package com.app.taskmanagement.card.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.taskmanagement.card.entity.CardAttachment;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for CardAttachment.
 */
public interface CardAttachmentRepository extends JpaRepository<CardAttachment, Integer> {

	/**
	 * Get all attachments for a card — ordered by upload time (newest first).
	 */
	List<CardAttachment> findAllByCardCardIdOrderByUploadedAtDesc(int cardId);

	/**
	 * Find a specific attachment that belongs to a specific card. The cardId check
	 * prevents accessing attachments from other cards.
	 */
	Optional<CardAttachment> findByAttachmentIdAndCardCardId(int attachmentId, int cardId);

	/**
	 * Count attachments on a card — for the card summary view.
	 */
	int countByCardCardId(int cardId);
}