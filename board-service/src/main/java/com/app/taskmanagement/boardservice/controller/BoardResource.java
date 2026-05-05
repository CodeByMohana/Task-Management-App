package com.app.taskmanagement.boardservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.app.taskmanagement.boardservice.dto.*;
import com.app.taskmanagement.boardservice.service.BoardService;

import java.util.List;

/**
 * REST controller for board and list operations.
 *
 * Important note on workspaceMemberUserIds: In the full system, this list would
 * be fetched from workspace-service to check WORKSPACE visibility. For now we
 * accept it as a request param so you can test without inter-service calls.
 *
 * When API Gateway is added, the gateway will inject this automatically from
 * the workspace membership context.
 *
 * Base URL: /api/boards
 */
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Tag(name = "Board", description = "Board and list management endpoints")
public class BoardResource {

	private final BoardService boardService;

	// =========================================================================
	// BOARD CRUD
	// =========================================================================

	/**
	 * POST /api/boards Create a new board inside a workspace. The logged-in user
	 * becomes the first board ADMIN.
	 */
	@PostMapping
	@Operation(summary = "Create a new board", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardResponse> createBoard(@Valid @RequestBody CreateBoardRequest request,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createBoard(request, userId));
	}

	/**
	 * GET /api/boards/{boardId} Get a single board with all lists and members.
	 *
	 * @param workspaceMemberIds comma-separated userIds who are workspace members
	 *                           (needed to check WORKSPACE visibility). Example:
	 *                           ?workspaceMemberIds=1,2,3,4
	 */
	@GetMapping("/{boardId}")
	@Operation(summary = "Get board by ID", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardResponse> getBoard(@PathVariable int boardId,
			@RequestParam(required = false, defaultValue = "") List<Integer> workspaceMemberIds,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.getBoard(boardId, userId, workspaceMemberIds));
	}

	/**
	 * GET /api/boards/workspace/{workspaceId} Get all boards in a workspace that
	 * the user can see. Filters by visibility (PUBLIC / WORKSPACE / PRIVATE).
	 */
	@GetMapping("/workspace/{workspaceId}")
	@Operation(summary = "Get boards in a workspace", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<BoardResponse>> getBoardsByWorkspace(@PathVariable int workspaceId,
			@RequestParam(required = false, defaultValue = "") List<Integer> workspaceMemberIds,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.getBoardsByWorkspace(workspaceId, userId, workspaceMemberIds));
	}

	/**
	 * PUT /api/boards/{boardId} Update board name, description, background, or
	 * visibility. Only board ADMINs can do this.
	 */
	@PutMapping("/{boardId}")
	@Operation(summary = "Update board", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardResponse> updateBoard(@PathVariable int boardId,
			@Valid @RequestBody UpdateBoardRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.updateBoard(boardId, request, userId));
	}

	/**
	 * PATCH /api/boards/{boardId}/close Close a board — makes it read-only. Only
	 * ADMINs can do this.
	 */
	@PatchMapping("/{boardId}/close")
	@Operation(summary = "Close a board (read-only mode)", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardResponse> closeBoard(@PathVariable int boardId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.closeBoard(boardId, userId));
	}

	/**
	 * PATCH /api/boards/{boardId}/reopen Reopen a closed board. Only ADMINs can do
	 * this.
	 */
	@PatchMapping("/{boardId}/reopen")
	@Operation(summary = "Reopen a closed board", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardResponse> reopenBoard(@PathVariable int boardId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.reopenBoard(boardId, userId));
	}

	/**
	 * DELETE /api/boards/{boardId} Permanently delete a board and all its lists.
	 * Only the board CREATOR can do this.
	 */
	@DeleteMapping("/{boardId}")
	@Operation(summary = "Delete a board", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deleteBoard(@PathVariable int boardId, @AuthenticationPrincipal Integer userId) {

		boardService.deleteBoard(boardId, userId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * GET /api/boards/{boardId}/analytics Get board analytics: card counts per
	 * list, total members, etc. Only board members can see analytics.
	 */
	@GetMapping("/{boardId}/analytics")
	@Operation(summary = "Get board analytics", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardAnalyticsResponse> getBoardAnalytics(@PathVariable int boardId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.getBoardAnalytics(boardId, userId));
	}

	// =========================================================================
	// BOARD MEMBER MANAGEMENT
	// =========================================================================

	/**
	 * POST /api/boards/{boardId}/members Add a user to a board with a role (ADMIN /
	 * MEMBER / OBSERVER). Only board ADMINs can do this.
	 */
	@PostMapping("/{boardId}/members")
	@Operation(summary = "Add member to board", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardMemberResponse> addMember(@PathVariable int boardId,
			@Valid @RequestBody AddBoardMemberRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.status(HttpStatus.CREATED).body(boardService.addBoardMember(boardId, request, userId));
	}

	/**
	 * DELETE /api/boards/{boardId}/members/{targetUserId} Remove a user from the
	 * board. ADMINs can remove anyone. Members can remove themselves (leave).
	 */
	@DeleteMapping("/{boardId}/members/{targetUserId}")
	@Operation(summary = "Remove member from board", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> removeMember(@PathVariable int boardId, @PathVariable int targetUserId,
			@AuthenticationPrincipal Integer userId) {

		boardService.removeBoardMember(boardId, targetUserId, userId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * PATCH /api/boards/{boardId}/members/{targetUserId}/role?role=ADMIN Change a
	 * member's role. Only board ADMINs can do this.
	 */
	@PatchMapping("/{boardId}/members/{targetUserId}/role")
	@Operation(summary = "Update board member role", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardMemberResponse> updateMemberRole(@PathVariable int boardId,
			@PathVariable int targetUserId, @RequestParam String role, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.updateBoardMemberRole(boardId, targetUserId, role, userId));
	}

	// =========================================================================
	// LIST (COLUMN) MANAGEMENT
	// =========================================================================

	/**
	 * POST /api/boards/{boardId}/lists Create a new list (column) on the board.
	 * Position is auto-assigned as the last column. Requires MEMBER or ADMIN role.
	 */
	@PostMapping("/{boardId}/lists")
	@Operation(summary = "Create a list on a board", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardListResponse> createList(@PathVariable int boardId,
			@Valid @RequestBody CreateListRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createList(boardId, request, userId));
	}

	/**
	 * PUT /api/boards/{boardId}/lists/{listId} Rename a list. Requires MEMBER or
	 * ADMIN role.
	 */
	@PutMapping("/{boardId}/lists/{listId}")
	@Operation(summary = "Update a list", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardListResponse> updateList(@PathVariable int boardId, @PathVariable int listId,
			@Valid @RequestBody UpdateListRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.updateList(boardId, listId, request, userId));
	}

	/**
	 * PATCH /api/boards/{boardId}/lists/{listId}/reorder Move a list to a new
	 * horizontal position (drag-and-drop). Body: { "newPosition": 2 }
	 */
	@PatchMapping("/{boardId}/lists/{listId}/reorder")
	@Operation(summary = "Reorder a list (drag-and-drop)", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardListResponse> reorderList(@PathVariable int boardId, @PathVariable int listId,
			@Valid @RequestBody ReorderListRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.reorderList(boardId, listId, request, userId));
	}

	/**
	 * PATCH /api/boards/{boardId}/lists/{listId}/archive Archive a list — hides it
	 * from view but keeps it recoverable.
	 */
	@PatchMapping("/{boardId}/lists/{listId}/archive")
	@Operation(summary = "Archive a list", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardListResponse> archiveList(@PathVariable int boardId, @PathVariable int listId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.archiveList(boardId, listId, userId));
	}

	/**
	 * PATCH /api/boards/{boardId}/lists/{listId}/unarchive Restore an archived list
	 * back to the board.
	 */
	@PatchMapping("/{boardId}/lists/{listId}/unarchive")
	@Operation(summary = "Restore an archived list", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<BoardListResponse> unarchiveList(@PathVariable int boardId, @PathVariable int listId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.unarchiveList(boardId, listId, userId));
	}

	/**
	 * DELETE /api/boards/{boardId}/lists/{listId} Permanently delete a list (must
	 * be archived first). Only board ADMINs can do this.
	 */
	@DeleteMapping("/{boardId}/lists/{listId}")
	@Operation(summary = "Permanently delete an archived list", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deleteList(@PathVariable int boardId, @PathVariable int listId,
			@AuthenticationPrincipal Integer userId) {

		boardService.deleteList(boardId, listId, userId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * GET /api/boards/{boardId}/lists/archived Get all archived lists for the board
	 * archive panel.
	 */
	@GetMapping("/{boardId}/lists/archived")
	@Operation(summary = "Get archived lists", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<BoardListResponse>> getArchivedLists(@PathVariable int boardId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(boardService.getArchivedLists(boardId, userId));
	}
}