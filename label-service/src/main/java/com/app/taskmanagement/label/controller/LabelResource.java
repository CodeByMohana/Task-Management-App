package com.app.taskmanagement.label.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.app.taskmanagement.label.dto.*;
import com.app.taskmanagement.label.service.LabelService;

import java.util.List;

/**
 * REST controller for labels and checklists.
 *
 * HOW CONTROLLERS WORK: A controller is the entry point for HTTP requests. When
 * the API gateway forwards a request here, Spring reads the URL and HTTP
 * method, finds the matching method in this controller, and calls it.
 *
 * @RestController = @Controller + @ResponseBody
 * @Controller → this class handles HTTP requests
 * @ResponseBody → return values are automatically converted to JSON
 *
 *               @RequestMapping("/api/labels"): All URLs in this controller
 *               start with /api/labels The gateway routes /api/labels/** to
 *               this service.
 *
 *               URL STRUCTURE: Labels → /api/labels/boards/{boardId}/labels
 *               Card labels → /api/labels/cards/{cardId}/labels Checklists →
 *               /api/labels/cards/{cardId}/checklists Items →
 *               /api/labels/checklists/{checklistId}/items
 *
 * @AuthenticationPrincipal Integer userId: Spring automatically injects the
 *                          logged-in user's ID here. The
 *                          JwtAuthenticationFilter sets this when it validates
 *                          the token. We don't use it in every method
 *                          currently, but it's available for future ownership
 *                          checks (e.g. "only board members can create
 *                          labels").
 */
@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
@Tag(name = "Labels & Checklists", description = "Board label management and card checklist operations")
public class LabelResource {

	private final LabelService labelService;

	// =========================================================================
	// LABEL ENDPOINTS
	// =========================================================================

	/**
	 * POST /api/labels/boards/{boardId}/labels
	 *
	 * Create a new label on a board.
	 *
	 * Example request body: { "name": "Bug", "color": "#FF5733" }
	 *
	 * Example response (201 Created): { "labelId": 1, "boardId": 5, "name": "Bug",
	 * "color": "#FF5733", "createdAt": "..." }
	 *
	 * @Valid triggers validation on the LabelRequest body. If "name" is blank or
	 *        "color" is not a valid hex code, returns 400 with error details.
	 */
	@PostMapping("/boards/{boardId}/labels")
	@Operation(summary = "Create a label on a board", description = "Creates a new colour-coded label. Name must be unique on the board.", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<LabelResponse> createLabel(@PathVariable int boardId,
			@Valid @RequestBody LabelRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.status(HttpStatus.CREATED).body(labelService.createLabel(boardId, request));
	}

	/**
	 * GET /api/labels/boards/{boardId}/labels
	 *
	 * Get all labels available on a board. Called when the board loads so users can
	 * see which labels exist and apply them to cards.
	 *
	 * Example response: [ { "labelId": 1, "name": "Bug", "color": "#FF5733" }, {
	 * "labelId": 2, "name": "Feature", "color": "#28A745" } ]
	 */
	@GetMapping("/boards/{boardId}/labels")
	@Operation(summary = "Get all labels on a board", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<LabelResponse>> getLabelsByBoard(@PathVariable int boardId) {
		return ResponseEntity.ok(labelService.getLabelsByBoard(boardId));
	}

	/**
	 * PUT /api/labels/boards/{boardId}/labels/{labelId}
	 *
	 * Update a label's name or color. The boardId in the URL acts as a security
	 * check — you can only edit labels that belong to the board you're working
	 * with.
	 *
	 * Example request body: { "name": "Critical Bug", "color": "#CC0000" }
	 */
	@PutMapping("/boards/{boardId}/labels/{labelId}")
	@Operation(summary = "Update a label's name or color", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<LabelResponse> updateLabel(@PathVariable int boardId, @PathVariable int labelId,
			@Valid @RequestBody LabelRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(labelService.updateLabel(boardId, labelId, request));
	}

	/**
	 * DELETE /api/labels/boards/{boardId}/labels/{labelId}
	 *
	 * Permanently delete a label from a board. Also removes this label from ALL
	 * cards on the board.
	 *
	 * Returns 204 No Content on success (no body — there's nothing left to return).
	 */
	@DeleteMapping("/boards/{boardId}/labels/{labelId}")
	@Operation(summary = "Delete a label from a board (also removes it from all cards)", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deleteLabel(@PathVariable int boardId, @PathVariable int labelId,
			@AuthenticationPrincipal Integer userId) {

		labelService.deleteLabel(boardId, labelId);
		return ResponseEntity.noContent().build(); // 204 No Content
	}

	// =========================================================================
	// CARD-LABEL ASSOCIATION ENDPOINTS
	// =========================================================================

	/**
	 * POST /api/labels/cards/{cardId}/labels/{labelId}
	 *
	 * Apply a label to a card. Example: apply "Bug" (labelId=1) to card 10.
	 *
	 * Returns 204 No Content — the operation succeeded, nothing to return. The
	 * client already knows the label details from the GET /boards/{boardId}/labels
	 * call.
	 */
	@PostMapping("/cards/{cardId}/labels/{labelId}")
	@Operation(summary = "Add a label to a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> addLabelToCard(@PathVariable int cardId, @PathVariable int labelId,
			@AuthenticationPrincipal Integer userId) {

		labelService.addLabelToCard(cardId, labelId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * DELETE /api/labels/cards/{cardId}/labels/{labelId}
	 *
	 * Remove a label from a card. This only removes the card-label link — the label
	 * itself is NOT deleted.
	 *
	 * Returns 204 No Content on success.
	 */
	@DeleteMapping("/cards/{cardId}/labels/{labelId}")
	@Operation(summary = "Remove a label from a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> removeLabelFromCard(@PathVariable int cardId, @PathVariable int labelId,
			@AuthenticationPrincipal Integer userId) {

		labelService.removeLabelFromCard(cardId, labelId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * GET /api/labels/cards/{cardId}/labels
	 *
	 * Get all labels currently applied to a specific card. Called when the card
	 * detail modal loads to show its labels.
	 *
	 * Example response: [{ "labelId": 1, "name": "Bug", "color": "#FF5733" }]
	 */
	@GetMapping("/cards/{cardId}/labels")
	@Operation(summary = "Get all labels on a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<LabelResponse>> getLabelsForCard(@PathVariable int cardId) {
		return ResponseEntity.ok(labelService.getLabelsForCard(cardId));
	}

	// =========================================================================
	// CHECKLIST ENDPOINTS
	// =========================================================================

	/**
	 * POST /api/labels/cards/{cardId}/checklists
	 *
	 * Create a new checklist on a card. Example request body: { "title": "Backend
	 * tasks" }
	 *
	 * Returns 201 Created with the new checklist (no items yet).
	 */
	@PostMapping("/cards/{cardId}/checklists")
	@Operation(summary = "Create a checklist on a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<ChecklistResponse> createChecklist(@PathVariable int cardId,
			@Valid @RequestBody CreateChecklistRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.status(HttpStatus.CREATED).body(labelService.createChecklist(cardId, request));
	}

	/**
	 * GET /api/labels/cards/{cardId}/checklists
	 *
	 * Get all checklists for a card, each with their items and progress summary.
	 * Called when the card detail modal opens.
	 *
	 * Example response: [{ "checklistId": 1, "title": "Backend tasks", "items": [{
	 * "text": "Write tests", "completed": false }], "completedCount": 0,
	 * "totalCount": 1, "progressPercent": 0 }]
	 */
	@GetMapping("/cards/{cardId}/checklists")
	@Operation(summary = "Get all checklists for a card", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<ChecklistResponse>> getChecklistsByCard(@PathVariable int cardId) {
		return ResponseEntity.ok(labelService.getChecklistsByCard(cardId));
	}

	/**
	 * DELETE /api/labels/checklists/{checklistId}
	 *
	 * Delete a checklist and ALL its items. Returns 204 No Content on success.
	 */
	@DeleteMapping("/checklists/{checklistId}")
	@Operation(summary = "Delete a checklist and all its items", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deleteChecklist(@PathVariable int checklistId,
			@AuthenticationPrincipal Integer userId) {

		labelService.deleteChecklist(checklistId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * GET /api/labels/checklists/{checklistId}/progress
	 *
	 * Get a checklist's current progress (completed/total/percent). Called after a
	 * user ticks an item to refresh the progress bar without reloading the whole
	 * card.
	 *
	 * Example response: { "completedCount": 2, "totalCount": 5, "progressPercent":
	 * 40 }
	 */
	@GetMapping("/checklists/{checklistId}/progress")
	@Operation(summary = "Get checklist progress (completed vs total items)", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<ChecklistResponse> getChecklistProgress(@PathVariable int checklistId) {
		return ResponseEntity.ok(labelService.getChecklistProgress(checklistId));
	}

	// =========================================================================
	// CHECKLIST ITEM ENDPOINTS
	// =========================================================================

	/**
	 * POST /api/labels/checklists/{checklistId}/items
	 *
	 * Add a new item to the bottom of a checklist.
	 *
	 * Example request body: { "text": "Write unit tests" } { "text": "Deploy to
	 * staging", "assigneeUserId": 5, "dueDate": "2026-06-01" }
	 *
	 * Returns 201 Created with the new item.
	 */
	@PostMapping("/checklists/{checklistId}/items")
	@Operation(summary = "Add an item to a checklist", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<ChecklistItemResponse> addItem(@PathVariable int checklistId,
			@Valid @RequestBody AddChecklistItemRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.status(HttpStatus.CREATED).body(labelService.addItem(checklistId, request));
	}

	/**
	 * PUT /api/labels/items/{itemId}/toggle
	 *
	 * Toggle a checklist item between done (✓) and not done (□). This is the most
	 * frequently called endpoint — every time a user ticks or un-ticks a checkbox
	 * in the card modal.
	 *
	 * Returns 200 OK with the updated item (so the client can update its UI state).
	 */
	@PutMapping("/items/{itemId}/toggle")
	@Operation(summary = "Toggle a checklist item done/not done", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<ChecklistItemResponse> toggleItem(@PathVariable int itemId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(labelService.toggleItem(itemId));
	}

	/**
	 * DELETE /api/labels/items/{itemId}
	 *
	 * Permanently delete a single checklist item. Returns 204 No Content on
	 * success.
	 */
	@DeleteMapping("/items/{itemId}")
	@Operation(summary = "Delete a checklist item", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deleteItem(@PathVariable int itemId, @AuthenticationPrincipal Integer userId) {

		labelService.deleteItem(itemId);
		return ResponseEntity.noContent().build();
	}
}