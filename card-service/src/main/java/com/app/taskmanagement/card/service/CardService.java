package com.app.taskmanagement.card.service;

import java.util.List;

import com.app.taskmanagement.card.dto.AddAttachmentRequest;
import com.app.taskmanagement.card.dto.CardAttachmentResponse;
import com.app.taskmanagement.card.dto.CardResponse;
import com.app.taskmanagement.card.dto.CreateCardRequest;
import com.app.taskmanagement.card.dto.MoveCardRequest;
import com.app.taskmanagement.card.dto.UpdateCardRequest;

/**
 * Contract for all card and attachment operations.
 */
public interface CardService {

	// ─── Card CRUD ────────────────────────────────────────────────────────────

	/**
	 * Create a new card in a list. Position is auto-assigned as last + 1 within the
	 * list.
	 *
	 * @param request       card details from client
	 * @param creatorUserId userId from JWT
	 */
	CardResponse createCard(CreateCardRequest request, int creatorUserId);

	/**
	 * Get a single card with full details including attachments.
	 *
	 * @param cardId           the card to fetch
	 * @param requestingUserId used for access logging (future use)
	 */
	CardResponse getCard(int cardId, int requestingUserId);

	/**
	 * Get all active cards in a list, ordered by position (top to bottom). This
	 * populates the cards in a board column.
	 */
	List<CardResponse> getCardsByList(int listId);

	/**
	 * Get all active cards on a board — for board-level views.
	 */
	List<CardResponse> getCardsByBoard(int boardId);

	/**
	 * Get all cards assigned to the requesting user across all boards. Used for a
	 * personal "My Tasks" dashboard.
	 */
	List<CardResponse> getMyCards(int userId);

	/**
	 * Update card fields (title, description, priority, status, assignee, dates).
	 * PATCH-style — only non-null fields are updated. Only the card creator or
	 * board members can update.
	 */
	CardResponse updateCard(int cardId, UpdateCardRequest request, int requestingUserId);

	/**
	 * Move a card to a different list or reorder it within the same list. Handles
	 * position shifting of other cards automatically.
	 *
	 * @param cardId           card to move
	 * @param request          target listId + new position
	 * @param requestingUserId must be a board member (not OBSERVER)
	 */
	CardResponse moveCard(int cardId, MoveCardRequest request, int requestingUserId);

	/**
	 * Archive a card — hides it from the board but keeps it recoverable.
	 */
	CardResponse archiveCard(int cardId, int requestingUserId);

	/**
	 * Restore an archived card back to its list (appended at the end).
	 */
	CardResponse unarchiveCard(int cardId, int requestingUserId);

	/**
	 * Permanently delete a card (must be archived first).
	 */
	void deleteCard(int cardId, int requestingUserId);

	/**
	 * Get all archived cards in a list — for the archive panel.
	 */
	List<CardResponse> getArchivedCards(int listId, int requestingUserId);

	/**
	 * Search cards by title within a board.
	 */
	List<CardResponse> searchCards(int boardId, String title);

	/**
	 * Get all cards assigned to a specific user within a board.
	 */
	List<CardResponse> getCardsByAssignee(int boardId, int assigneeUserId);

	// ─── Attachment Operations ────────────────────────────────────────────────

	/**
	 * Register a file attachment on a card. The file itself is already uploaded to
	 * S3 — this saves the metadata.
	 */
	CardAttachmentResponse addAttachment(int cardId, AddAttachmentRequest request, int userId);

	/**
	 * Get all attachments for a card.
	 */
	List<CardAttachmentResponse> getAttachments(int cardId);

	/**
	 * Delete an attachment record (does NOT delete the file from S3 — handle
	 * separately). Only the uploader or a board admin can delete.
	 */
	void deleteAttachment(int cardId, int attachmentId, int requestingUserId);
}
