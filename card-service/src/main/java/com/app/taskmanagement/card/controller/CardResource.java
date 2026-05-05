package com.app.taskmanagement.card.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.app.taskmanagement.card.dto.*;
import com.app.taskmanagement.card.exception.BadRequestException;
import com.app.taskmanagement.card.service.CardService;

import java.util.List;

/**
 * REST controller for card and attachment operations.
 *
 * Base URL: /api/cards
 *
 * All endpoints require authentication via JWT (except where explicitly noted).
 * userId is extracted automatically from the JWT token
 * via @AuthenticationPrincipal.
 */
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Card", description = "Card and attachment management endpoints")
public class CardResource {

	private final CardService cardService;

	// =========================================================================
	// CARD CRUD — CREATE
	// =========================================================================

	/**
	 * POST /api/cards Create a new card in a list. The logged-in user becomes the
	 * card creator. Position is auto-assigned as the last card in the list.
	 */
	@PostMapping
	@Operation(summary = "Create a new card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request, userId));
	}

	// =========================================================================
	// CARD CRUD — READ (GET by various filters)
	// =========================================================================

	/**
	 * GET /api/cards/{cardId} Get a single card with full details including
	 * attachments.
	 */
	@GetMapping("/{cardId}")
	@Operation(summary = "Get card by ID", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CardResponse> getCard(@PathVariable int cardId, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(cardService.getCard(cardId, userId));
	}

	/**
	 * GET /api/cards/list/{listId} Get all active cards in a list, ordered by
	 * position (top to bottom). This is what populates the cards in a board column.
	 */
	@GetMapping("/list/{listId}")
	@Operation(summary = "Get cards by list", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<CardResponse>> getCardsByList(@PathVariable int listId) {
		return ResponseEntity.ok(cardService.getCardsByList(listId));
	}

	/**
	 * GET /api/cards/board/{boardId} Get all active cards on a board. Used for
	 * board-level views and analytics.
	 */
	@GetMapping("/board/{boardId}")
	@Operation(summary = "Get cards by board", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<CardResponse>> getCardsByBoard(@PathVariable int boardId) {
		return ResponseEntity.ok(cardService.getCardsByBoard(boardId));
	}

	/**
	 * GET /api/cards/assignee/{boardId}/{assigneeUserId} Get all cards assigned to
	 * a specific user within a board. Used for filtering: "Show me all cards
	 * assigned to John on this board"
	 */
	@GetMapping("/assignee/{boardId}/{assigneeUserId}")
	@Operation(summary = "Get cards by assignee in a board", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<CardResponse>> getCardsByAssignee(@PathVariable int boardId,
			@PathVariable int assigneeUserId) {

		return ResponseEntity.ok(cardService.getCardsByAssignee(boardId, assigneeUserId));
	}

	/**
	 * GET /api/cards/my-cards Get all cards assigned to the logged-in user across
	 * all boards. Powers the personal "My Tasks" dashboard.
	 */
	@GetMapping("/my-cards")
	@Operation(summary = "Get my assigned cards", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<CardResponse>> getMyCards(@AuthenticationPrincipal Integer userId) {
		return ResponseEntity.ok(cardService.getMyCards(userId));
	}

	/**
	 * GET /api/cards/search/{boardId}?title=bug Search cards by title within a
	 * board. Case-insensitive partial match (SQL LIKE '%title%').
	 */
	@GetMapping("/search/{boardId}")
	@Operation(summary = "Search cards by title", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<CardResponse>> searchCards(@PathVariable int boardId, @RequestParam String title) {

		return ResponseEntity.ok(cardService.searchCards(boardId, title));
	}

	/**
	 * GET /api/cards/list/{listId}/archived Get all archived cards in a list — for
	 * the archive panel.
	 */
	@GetMapping("/list/{listId}/archived")
	@Operation(summary = "Get archived cards in a list", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<CardResponse>> getArchivedCards(@PathVariable int listId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(cardService.getArchivedCards(listId, userId));
	}

	// =========================================================================
	// CARD CRUD — UPDATE
	// =========================================================================

	/**
	 * PUT /api/cards/{cardId} Update card fields: title, description, priority,
	 * status, assignee, dates, cover. PATCH-style: only non-null fields are
	 * updated. Body example: { "title": "New Title", "priority": "HIGH" }
	 */
	@PutMapping("/{cardId}")
	@Operation(summary = "Update card details", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CardResponse> updateCard(@PathVariable int cardId,
			@Valid @RequestBody UpdateCardRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(cardService.updateCard(cardId, request, userId));
	}

	/**
	 * PUT /api/cards/{cardId}/move Move a card to a different list OR reorder
	 * within the same list. Body: { "targetListId": 5, "newPosition": 2 }
	 *
	 * This handles drag-and-drop: - Dragging between columns → different listId -
	 * Dragging within a column → same listId, different position
	 */
	@PutMapping("/{cardId}/move")
	@Operation(summary = "Move or reorder a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CardResponse> moveCard(@PathVariable int cardId, @Valid @RequestBody MoveCardRequest request,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(cardService.moveCard(cardId, request, userId));
	}

	/**
	 * PUT /api/cards/{cardId}/assignee?userId=5 Assign a card to a user, or
	 * unassign by passing userId=0. Convenience endpoint — same as calling
	 * updateCard with assigneeUserId field.
	 */
	@PutMapping("/{cardId}/assignee")
	@Operation(summary = "Assign or unassign a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CardResponse> updateAssignee(@PathVariable int cardId, @RequestParam int targetUserId,
			@AuthenticationPrincipal Integer userId) {

		UpdateCardRequest request = new UpdateCardRequest();
		request.setAssigneeUserId(targetUserId == 0 ? 0 : targetUserId);

		return ResponseEntity.ok(cardService.updateCard(cardId, request, userId));
	}

	/**
	 * PUT /api/cards/{cardId}/priority?priority=HIGH Update only the card's
	 * priority. Convenience endpoint — same as calling updateCard with priority
	 * field.
	 */
	@PutMapping("/{cardId}/priority")
	@Operation(summary = "Update card priority", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CardResponse> updatePriority(@PathVariable int cardId, @RequestParam String priority,
			@AuthenticationPrincipal Integer userId) {

		UpdateCardRequest request = new UpdateCardRequest();
		try {
			request.setPriority(com.app.taskmanagement.card.entity.Card.Priority.valueOf(priority.toUpperCase()));
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Invalid priority. Must be LOW, MEDIUM, HIGH, or CRITICAL");
		}

		return ResponseEntity.ok(cardService.updateCard(cardId, request, userId));
	}

	// =========================================================================
	// CARD CRUD — ARCHIVE / UNARCHIVE / DELETE
	// =========================================================================

	/**
	 * POST /api/cards/{cardId}/archive Archive a card — soft-delete, hidden from
	 * board but recoverable.
	 */
	@PostMapping("/{cardId}/archive")
	@Operation(summary = "Archive a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CardResponse> archiveCard(@PathVariable int cardId, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(cardService.archiveCard(cardId, userId));
	}

	/**
	 * POST /api/cards/{cardId}/unarchive Restore an archived card back to its list
	 * (appended at the end).
	 */
	@PostMapping("/{cardId}/unarchive")
	@Operation(summary = "Unarchive a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CardResponse> unarchiveCard(@PathVariable int cardId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(cardService.unarchiveCard(cardId, userId));
	}

	/**
	 * DELETE /api/cards/{cardId} Permanently delete a card (must be archived
	 * first). Safety guard prevents accidental deletion of active cards.
	 */
	@DeleteMapping("/{cardId}")
	@Operation(summary = "Permanently delete a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deleteCard(@PathVariable int cardId, @AuthenticationPrincipal Integer userId) {

		cardService.deleteCard(cardId, userId);
		return ResponseEntity.noContent().build();
	}

	// =========================================================================
	// ATTACHMENT OPERATIONS
	// =========================================================================

	/**
	 * POST /api/cards/{cardId}/attachments Register a file attachment on a card.
	 * The actual file is already uploaded to S3 — this saves the metadata.
	 *
	 * Flow: 1. Frontend gets a pre-signed S3 URL 2. Frontend uploads file directly
	 * to S3 3. Frontend calls this endpoint with metadata (fileName, fileUrl, etc.)
	 * 4. card-service saves the attachment record
	 */
	@PostMapping("/{cardId}/attachments")
	@Operation(summary = "Add attachment to card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<CardAttachmentResponse> addAttachment(@PathVariable int cardId,
			@Valid @RequestBody AddAttachmentRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.status(HttpStatus.CREATED).body(cardService.addAttachment(cardId, request, userId));
	}

	/**
	 * GET /api/cards/{cardId}/attachments Get all attachments for a card.
	 */
	@GetMapping("/{cardId}/attachments")
	@Operation(summary = "Get card attachments", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<CardAttachmentResponse>> getAttachments(@PathVariable int cardId) {
		return ResponseEntity.ok(cardService.getAttachments(cardId));
	}

	/**
	 * DELETE /api/cards/{cardId}/attachments/{attachmentId} Delete an attachment
	 * record. Only the person who uploaded it can delete. Note: actual S3 file
	 * deletion should be handled separately.
	 */
	@DeleteMapping("/{cardId}/attachments/{attachmentId}")
	@Operation(summary = "Delete an attachment", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deleteAttachment(@PathVariable int cardId, @PathVariable int attachmentId,
			@AuthenticationPrincipal Integer userId) {

		cardService.deleteAttachment(cardId, attachmentId, userId);
		return ResponseEntity.noContent().build();
	}
}