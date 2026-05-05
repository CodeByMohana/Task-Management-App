package com.app.taskmanagement.boardservice.service;

import java.util.List;

import com.app.taskmanagement.boardservice.dto.*;

/**
 * Contract for board and list business logic.
 *
 * Separating interface from implementation lets us: - Swap implementations
 * easily (e.g. mock for testing) - Keep controllers clean — they depend on the
 * interface, not the impl - Make Spring inject the correct bean automatically
 */
public interface BoardService {

	// ─── Board Operations ─────────────────────────────────────────────────────

	/**
	 * Create a new board inside a workspace. The creator automatically becomes the
	 * first board ADMIN.
	 *
	 * @param request       board details from client
	 * @param creatorUserId userId from JWT (who is creating it)
	 */
	BoardResponse createBoard(CreateBoardRequest request, int creatorUserId);

	/**
	 * Get a single board with all its lists and members. Access control: PUBLIC =
	 * anyone, WORKSPACE = workspace members, PRIVATE = board members.
	 *
	 * @param boardId                the board to fetch
	 * @param requestingUserId       userId from JWT (used for access check)
	 * @param workspaceMemberUserIds list of userIds in the workspace (for WORKSPACE
	 *                               visibility check)
	 */
	BoardResponse getBoard(int boardId, int requestingUserId, List<Integer> workspaceMemberUserIds);

	/**
	 * Get all accessible boards in a workspace for the requesting user.
	 *
	 * @param workspaceId            the workspace to list boards for
	 * @param requestingUserId       the logged-in user
	 * @param workspaceMemberUserIds all member userIds of the workspace
	 */
	List<BoardResponse> getBoardsByWorkspace(int workspaceId, int requestingUserId,
			List<Integer> workspaceMemberUserIds);

	/**
	 * Update board name, description, background, or visibility. Only board ADMINs
	 * can do this.
	 */
	BoardResponse updateBoard(int boardId, UpdateBoardRequest request, int requestingUserId);

	/**
	 * Close a board (read-only mode — no new cards/changes). Only board ADMINs can
	 * close a board.
	 */
	BoardResponse closeBoard(int boardId, int requestingUserId);

	/**
	 * Reopen a closed board. Only board ADMINs can reopen.
	 */
	BoardResponse reopenBoard(int boardId, int requestingUserId);

	/**
	 * Permanently delete a board and all its lists. Only the board creator can
	 * delete it.
	 */
	void deleteBoard(int boardId, int requestingUserId);

	// ─── Board Member Operations ───────────────────────────────────────────────

	/**
	 * Add a user to a board with a given role. Only board ADMINs can add members.
	 */
	BoardMemberResponse addBoardMember(int boardId, AddBoardMemberRequest request, int requestingUserId);

	/**
	 * Remove a user from a board. ADMINs can remove anyone. Members can remove
	 * themselves (leave).
	 */
	void removeBoardMember(int boardId, int targetUserId, int requestingUserId);

	/**
	 * Change a board member's role (MEMBER ↔ ADMIN ↔ OBSERVER). Only board ADMINs
	 * can change roles.
	 */
	BoardMemberResponse updateBoardMemberRole(int boardId, int targetUserId, String newRole, int requestingUserId);

	/**
	 * Get analytics for a board: card counts per list, overdue cards, completion
	 * rate. Only board members can access analytics.
	 */
	BoardAnalyticsResponse getBoardAnalytics(int boardId, int requestingUserId);

	// ─── List (Column) Operations ──────────────────────────────────────────────

	/**
	 * Create a new list (column) on a board. Position is auto-assigned as last + 1.
	 * Only board MEMBERs or ADMINs can add lists.
	 */
	BoardListResponse createList(int boardId, CreateListRequest request, int requestingUserId);

	/**
	 * Rename a list. Only board MEMBERs or ADMINs can rename.
	 */
	BoardListResponse updateList(int boardId, int listId, UpdateListRequest request, int requestingUserId);

	/**
	 * Move a list to a new horizontal position (drag-and-drop). Other lists shift
	 * to accommodate the moved one.
	 */
	BoardListResponse reorderList(int boardId, int listId, ReorderListRequest request, int requestingUserId);

	/**
	 * Archive a list — hides it from board view but keeps it recoverable.
	 */
	BoardListResponse archiveList(int boardId, int listId, int requestingUserId);

	/**
	 * Restore an archived list back to the board.
	 */
	BoardListResponse unarchiveList(int boardId, int listId, int requestingUserId);

	/**
	 * Permanently delete a list and all its cards. Only archived lists can be
	 * permanently deleted.
	 */
	void deleteList(int boardId, int listId, int requestingUserId);

	/**
	 * Get all archived lists for a board (for the archive panel).
	 */
	List<BoardListResponse> getArchivedLists(int boardId, int requestingUserId);
}