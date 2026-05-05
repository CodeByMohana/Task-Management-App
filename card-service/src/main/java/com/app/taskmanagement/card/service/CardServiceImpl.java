package com.app.taskmanagement.card.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.app.taskmanagement.card.dto.*;
import com.app.taskmanagement.card.entity.Card;
import com.app.taskmanagement.card.entity.CardAttachment;
import com.app.taskmanagement.card.exception.*;
import com.app.taskmanagement.card.repository.CardAttachmentRepository;
import com.app.taskmanagement.card.repository.CardRepository;
import com.app.taskmanagement.card.messaging.NotificationPublisher;

import java.util.List;

/**
 * Implementation of all card and attachment business logic.
 *
 * Key concepts:
 *
 * POSITION MANAGEMENT: Cards in a list are ordered by an integer 'position'
 * field. When a card moves, we shift other cards' positions to fill gaps. Think
 * of it like inserting into a sorted array — other elements shift.
 *
 * Example — moving card from position 4 to position 2 in same list: Before:
 * [Card A:1, Card B:2, Card C:3, Card D:4, Card E:5] After: [Card A:1, Card
 * D:2, Card B:3, Card C:4, Card E:5]
 *
 * CROSS-LIST MOVE: When a card moves to a different list: 1. Shift DOWN
 * positions in the old list (fill the gap left by the card) 2. Shift UP
 * positions in the new list (make room for the incoming card) 3. Set card's
 * listId and position to the new values
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CardServiceImpl implements CardService {

	private final CardRepository cardRepository;
	private final CardAttachmentRepository attachmentRepository;
	private final NotificationPublisher notificationPublisher;

	// =========================================================================
	// CARD CRUD
	// =========================================================================

	@Override
	public CardResponse createCard(CreateCardRequest request, int creatorUserId) {

		// Auto-assign position as last + 1 in the target list
		int nextPosition = cardRepository.findMaxPositionByListId(request.getListId()) + 1;

		Card card = Card.builder().listId(request.getListId()).boardId(request.getBoardId())
				.workspaceId(request.getWorkspaceId()).title(request.getTitle()).description(request.getDescription())
				.priority(request.getPriority() != null ? request.getPriority() : Card.Priority.MEDIUM)
				.status(Card.Status.TO_DO) // new cards always start as TO_DO
				.assigneeUserId(request.getAssigneeUserId()).createdByUserId(creatorUserId)
				.dueDate(request.getDueDate()).startDate(request.getStartDate()).coverColor(request.getCoverColor())
				.position(nextPosition).build();

		Card savedCard = cardRepository.save(card);
		
		// Trigger notification if assigned
		if (savedCard.getAssigneeUserId() != null && savedCard.getAssigneeUserId() != creatorUserId) {
			notificationPublisher.notifyCardAssigned(
				savedCard.getAssigneeUserId(),
				savedCard.getTitle(),
				creatorUserId,
				(long) savedCard.getCardId(),
				String.valueOf(savedCard.getWorkspaceId()),
				request.getBoardName(),
				request.getWorkspaceName()
			);
		}

		return CardResponse.from(savedCard, false);
	}

	@Override
	@Transactional(readOnly = true)
	public CardResponse getCard(int cardId, int requestingUserId) {
		// includeDetails = true → returns full attachment list for the detail view
		return CardResponse.from(findCardById(cardId), true);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CardResponse> getCardsByList(int listId) {
		return cardRepository.findAllByListIdAndArchivedFalseOrderByPositionAsc(listId).stream()
				.map(card -> CardResponse.from(card, false)) // lightweight for board view
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CardResponse> getCardsByBoard(int boardId) {
		return cardRepository.findAllByBoardIdAndArchivedFalse(boardId).stream()
				.map(card -> CardResponse.from(card, false)).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CardResponse> getMyCards(int userId) {
		return cardRepository.findAllByAssigneeUserIdAndArchivedFalse(userId).stream()
				.map(card -> CardResponse.from(card, false)).toList();
	}

	@Override
	public CardResponse updateCard(int cardId, UpdateCardRequest request, int requestingUserId) {
		Card card = findCardById(cardId);

		/*
		 * PATCH-style update: only update fields that were actually provided.
		 * StringUtils.hasText() = not null AND not empty AND not just whitespace.
		 */
		if (StringUtils.hasText(request.getTitle()))
			card.setTitle(request.getTitle());
		if (request.getDescription() != null)
			card.setDescription(request.getDescription());
		if (request.getPriority() != null)
			card.setPriority(request.getPriority());
		if (request.getStatus() != null)
			card.setStatus(request.getStatus());
		if (request.getDueDate() != null)
			card.setDueDate(request.getDueDate());
		if (request.getStartDate() != null)
			card.setStartDate(request.getStartDate());
		if (request.getCoverColor() != null)
			card.setCoverColor(request.getCoverColor());

		/*
		 * Assignee update: null → don't change the assignee 0 → unassign (set to null)
		 * userId → assign to that user
		 */
		if (request.getAssigneeUserId() != null) {
			card.setAssigneeUserId(request.getAssigneeUserId() == 0 ? null : request.getAssigneeUserId());
		}

		Card savedCard = cardRepository.save(card);
		
		// Trigger notification if assignee was changed
		if (request.getAssigneeUserId() != null && request.getAssigneeUserId() != 0 && request.getAssigneeUserId() != requestingUserId) {
			notificationPublisher.notifyCardAssigned(
				savedCard.getAssigneeUserId(),
				savedCard.getTitle(),
				requestingUserId,
				(long) savedCard.getCardId(),
				String.valueOf(savedCard.getWorkspaceId()),
				request.getBoardName(),
				request.getWorkspaceName()
			);
		}

		return CardResponse.from(savedCard, true);
	}

	@Override
	public CardResponse moveCard(int cardId, MoveCardRequest request, int requestingUserId) {
		Card card = findCardById(cardId);

		int oldListId = card.getListId();
		int newListId = request.getTargetListId();
		int oldPosition = card.getPosition();
		int newPosition = request.getNewPosition();

		boolean movingToSameList = (oldListId == newListId);

		if (movingToSameList) {
			/*
			 * Reordering within the same list.
			 *
			 * Case 1: Moving DOWN (e.g. pos 2 → pos 4) Cards between old and new position
			 * shift UP by 1 Example: [A:1, B:2, C:3, D:4] move B to 4 → C:3→2, D:4→3, B
			 * becomes 4 → [A:1, C:2, D:3, B:4]
			 *
			 * Case 2: Moving UP (e.g. pos 4 → pos 2) Cards between new and old position
			 * shift DOWN by 1 Example: [A:1, B:2, C:3, D:4] move D to 2 → B:2→3, C:3→4, D
			 * becomes 2 → [A:1, D:2, B:3, C:4]
			 *
			 * shiftPositionsUp handles making room at newPosition. We first shift, then
			 * place the card.
			 */
			if (newPosition != oldPosition) {
				cardRepository.shiftPositionsUp(oldListId, newPosition, cardId);
				card.setPosition(newPosition);
			}
		} else {
			/*
			 * Moving to a DIFFERENT list.
			 *
			 * Step 1: Close the gap in the OLD list Cards after the card's old position
			 * shift down by 1
			 *
			 * Step 2: Make room in the NEW list Cards at or after newPosition shift up by 1
			 *
			 * Step 3: Place the card in the new list at newPosition
			 */

			// Step 1: fill gap in old list
			cardRepository.shiftPositionsDown(oldListId, oldPosition, cardId);

			// Step 2: make room in new list
			cardRepository.shiftPositionsUp(newListId, newPosition, cardId);

			// Step 3: update the card's list and position
			card.setListId(newListId);
			card.setPosition(newPosition);

			// Update status to IN_PROGRESS when moved out of TO_DO list
			// (optional UX enhancement — can be removed if not desired)
			if (card.getStatus() == Card.Status.TO_DO) {
				card.setStatus(Card.Status.IN_PROGRESS);
			}
		}

		return CardResponse.from(cardRepository.save(card), false);
	}

	@Override
	public CardResponse archiveCard(int cardId, int requestingUserId) {
		Card card = findCardById(cardId);

		if (card.isArchived()) {
			throw new BadRequestException("Card is already archived");
		}

		card.setArchived(true);
		return CardResponse.from(cardRepository.save(card), false);
	}

	@Override
	public CardResponse unarchiveCard(int cardId, int requestingUserId) {
		Card card = findCardById(cardId);

		if (!card.isArchived()) {
			throw new BadRequestException("Card is not archived");
		}

		/*
		 * On unarchive, append to the end of the list it was in. Avoids position
		 * conflicts with currently visible cards.
		 */
		int nextPosition = cardRepository.findMaxPositionByListId(card.getListId()) + 1;
		card.setArchived(false);
		card.setPosition(nextPosition);

		return CardResponse.from(cardRepository.save(card), false);
	}

	@Override
	public void deleteCard(int cardId, int requestingUserId) {
		Card card = findCardById(cardId);

		// Safety guard: only archived cards can be permanently deleted
		if (!card.isArchived()) {
			throw new BadRequestException("Archive the card first before permanently deleting it");
		}

		// CascadeType.ALL on attachments means attachment records are auto-deleted
		cardRepository.delete(card);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CardResponse> getArchivedCards(int listId, int requestingUserId) {
		return cardRepository.findAllByListIdAndArchivedTrueOrderByPositionAsc(listId).stream()
				.map(card -> CardResponse.from(card, false)).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CardResponse> searchCards(int boardId, String title) {
		return cardRepository.findByBoardIdAndTitleContainingIgnoreCaseAndArchivedFalse(boardId, title).stream()
				.map(card -> CardResponse.from(card, false)).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CardResponse> getCardsByAssignee(int boardId, int assigneeUserId) {
		return cardRepository.findByBoardIdAndAssigneeUserIdAndArchivedFalse(boardId, assigneeUserId).stream()
				.map(card -> CardResponse.from(card, false)).toList();
	}

	// =========================================================================
	// ATTACHMENT OPERATIONS
	// =========================================================================

	@Override
	public CardAttachmentResponse addAttachment(int cardId, AddAttachmentRequest request, int userId) {
		Card card = findCardById(cardId);

		if (card.isArchived()) {
			throw new BadRequestException("Cannot add attachments to an archived card");
		}

		CardAttachment attachment = CardAttachment.builder().card(card).fileName(request.getFileName())
				.fileType(request.getFileType()).fileSize(request.getFileSize()).fileUrl(request.getFileUrl())
				.uploadedByUserId(userId).build();

		return CardAttachmentResponse.from(attachmentRepository.save(attachment));
	}

	@Override
	@Transactional(readOnly = true)
	public List<CardAttachmentResponse> getAttachments(int cardId) {
		findCardById(cardId); // verify card exists
		return attachmentRepository.findAllByCardCardIdOrderByUploadedAtDesc(cardId).stream()
				.map(CardAttachmentResponse::from).toList();
	}

	@Override
	public void deleteAttachment(int cardId, int attachmentId, int requestingUserId) {
		findCardById(cardId); // verify card exists

		CardAttachment attachment = attachmentRepository.findByAttachmentIdAndCardCardId(attachmentId, cardId)
				.orElseThrow(() -> new ResourceNotFoundException("Attachment not found on this card"));

		/*
		 * Only the person who uploaded it can delete the metadata record. Note: actual
		 * S3 file deletion should be handled by a separate storage service or triggered
		 * from here via an S3 client (future).
		 */
		if (attachment.getUploadedByUserId() != requestingUserId) {
			throw new ForbiddenException("Only the uploader can delete this attachment");
		}

		attachmentRepository.delete(attachment);
	}

	// =========================================================================
	// PRIVATE HELPERS
	// =========================================================================

	/**
	 * Fetch a card or throw 404. Centralizing this avoids repeating orElseThrow
	 * across all methods.
	 */
	private Card findCardById(int cardId) {
		return cardRepository.findById(cardId)
				.orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));
	}
}