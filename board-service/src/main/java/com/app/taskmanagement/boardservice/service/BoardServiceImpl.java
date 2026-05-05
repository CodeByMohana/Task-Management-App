package com.app.taskmanagement.boardservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.app.taskmanagement.boardservice.dto.*;
import com.app.taskmanagement.boardservice.entity.Board;
import com.app.taskmanagement.boardservice.entity.BoardList;
import com.app.taskmanagement.boardservice.entity.BoardMember;
import com.app.taskmanagement.boardservice.exception.*;
import com.app.taskmanagement.boardservice.repository.BoardListRepository;
import com.app.taskmanagement.boardservice.repository.BoardMemberRepository;
import com.app.taskmanagement.boardservice.repository.BoardRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of all board and list business logic.
 *
 * Key concepts used here: - @Transactional: wraps each method in a DB
 * transaction. If anything fails midway, the whole operation rolls back.
 * - @Transactional(readOnly = true): hint to DB that no writes happen. Improves
 * performance on SELECT-only methods. - Access control is enforced at the
 * service layer, not the controller. This is a best practice — security logic
 * should not live in controllers.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BoardServiceImpl implements BoardService {

	private final BoardRepository boardRepository;
	private final BoardMemberRepository boardMemberRepository;
	private final BoardListRepository boardListRepository;

	// =========================================================================
	// BOARD OPERATIONS
	// =========================================================================

	@Override
	public BoardResponse createBoard(CreateBoardRequest request, int creatorUserId) {

		// Build and save the board entity
		Board board = Board.builder().workspaceId(request.getWorkspaceId()).name(request.getName())
				.description(request.getDescription()).background(request.getBackground())
				.visibility(request.getVisibility()).createdByUserId(creatorUserId).build();

		board = boardRepository.save(board);

		/*
		 * Auto-add the creator as ADMIN of this board. Every board must have at least
		 * one ADMIN from the start.
		 */
		BoardMember creatorMember = BoardMember.builder().board(board).userId(creatorUserId)
				.role(BoardMember.Role.ADMIN).build();

		boardMemberRepository.save(creatorMember);
		board.getMembers().add(creatorMember);

		/*
		 * Auto-add workspace members as board MEMBER so they can immediately
		 * see and interact with the board. Skip the creator (already ADMIN).
		 */
		if (request.getWorkspaceMemberIds() != null) {
			for (Integer memberId : request.getWorkspaceMemberIds()) {
				if (memberId != creatorUserId) {
					BoardMember wsMember = BoardMember.builder().board(board).userId(memberId)
							.role(BoardMember.Role.MEMBER).build();
					boardMemberRepository.save(wsMember);
					board.getMembers().add(wsMember);
				}
			}
		}

		return BoardResponse.from(board, true);
	}

	@Override
	@Transactional(readOnly = true)
	public BoardResponse getBoard(int boardId, int requestingUserId, List<Integer> workspaceMemberUserIds) {
		Board board = findBoardById(boardId);
		/*
		 * Access check: allow if user is a direct board member, OR if the
		 * workspaceMemberIds list includes them, OR if the list is empty
		 * (trust that the caller has already verified workspace access).
		 */
		boolean isBoardMember = boardMemberRepository.existsByBoardBoardIdAndUserId(boardId, requestingUserId);
		boolean isWorkspaceMember = workspaceMemberUserIds.isEmpty() || workspaceMemberUserIds.contains(requestingUserId);
		if (!isBoardMember && !isWorkspaceMember && board.getVisibility() != Board.Visibility.PUBLIC) {
			throw new ForbiddenException("You do not have access to this board");
		}
		return BoardResponse.from(board, true);
	}

	@Override
	@Transactional(readOnly = true)
	public List<BoardResponse> getBoardsByWorkspace(int workspaceId, int requestingUserId,
			List<Integer> workspaceMemberUserIds) {
		/*
		 * Return ALL boards in the workspace. The workspace-service already controls
		 * who can access the workspace. If the user can see the workspace, they should
		 * see all boards inside it. This avoids the cross-service membership problem
		 * where the board-service cannot verify workspace membership on its own.
		 */
		List<Board> allBoards = boardRepository.findAllByWorkspaceId(workspaceId);
		return allBoards.stream()
				.map(board -> BoardResponse.from(board, false))
				.toList();
	}

	@Override
	public BoardResponse updateBoard(int boardId, UpdateBoardRequest request, int requestingUserId) {
		Board board = findBoardById(boardId);
		requireBoardAdmin(boardId, requestingUserId);
		requireBoardOpen(board);

		// PATCH-style: only update fields that were actually sent
		if (StringUtils.hasText(request.getName()))
			board.setName(request.getName());
		if (request.getDescription() != null)
			board.setDescription(request.getDescription());
		if (request.getBackground() != null)
			board.setBackground(request.getBackground());
		if (request.getVisibility() != null)
			board.setVisibility(request.getVisibility());

		return BoardResponse.from(boardRepository.save(board), true);
	}

	@Override
	public BoardResponse closeBoard(int boardId, int requestingUserId) {
		Board board = findBoardById(boardId);
		requireBoardAdmin(boardId, requestingUserId);

		if (board.isClosed()) {
			throw new BadRequestException("Board is already closed");
		}

		board.setClosed(true);
		return BoardResponse.from(boardRepository.save(board), false);
	}

	@Override
	public BoardResponse reopenBoard(int boardId, int requestingUserId) {
		Board board = findBoardById(boardId);
		requireBoardAdmin(boardId, requestingUserId);

		if (!board.isClosed()) {
			throw new BadRequestException("Board is already open");
		}

		board.setClosed(false);
		return BoardResponse.from(boardRepository.save(board), false);
	}

	@Override
	public void deleteBoard(int boardId, int requestingUserId) {
		Board board = findBoardById(boardId);

		/*
		 * Only the original creator can delete the board. Even other ADMINs cannot
		 * delete — prevents accidental data loss by promoted members.
		 */
		if (board.getCreatedByUserId() != requestingUserId) {
			throw new ForbiddenException("Only the board creator can delete this board");
		}

		// CascadeType.ALL on lists + members handles cleanup automatically
		boardRepository.delete(board);
	}

	// =========================================================================
	// BOARD MEMBER OPERATIONS
	// =========================================================================

	@Override
	public BoardMemberResponse addBoardMember(int boardId, AddBoardMemberRequest request, int requestingUserId) {
		findBoardById(boardId);
		requireBoardAdmin(boardId, requestingUserId);

		// Prevent duplicate membership
		if (boardMemberRepository.existsByBoardBoardIdAndUserId(boardId, request.getUserId())) {
			throw new DuplicateResourceException("User is already a member of this board");
		}

		Board boardRef = boardRepository.getReferenceById(boardId);
		BoardMember member = BoardMember.builder().board(boardRef).userId(request.getUserId()).role(request.getRole())
				.build();

		return BoardMemberResponse.from(boardMemberRepository.save(member));
	}

	@Override
	public void removeBoardMember(int boardId, int targetUserId, int requestingUserId) {
		findBoardById(boardId);

		BoardMember target = boardMemberRepository.findByBoardBoardIdAndUserId(boardId, targetUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Member not found on this board"));

		boolean isSelfRemoval = (targetUserId == requestingUserId);
		boolean isAdmin = isAdmin(boardId, requestingUserId);

		// Permission: only self-removal or ADMINs can remove members
		if (!isSelfRemoval && !isAdmin) {
			throw new ForbiddenException("Only board admins can remove other members");
		}

		// Prevent removing the last ADMIN
		if (target.getRole() == BoardMember.Role.ADMIN) {
			long adminCount = boardMemberRepository.findAllByBoardBoardId(boardId).stream()
					.filter(m -> m.getRole() == BoardMember.Role.ADMIN).count();
			if (adminCount <= 1) {
				throw new BadRequestException("Cannot remove the last admin. Promote another member first.");
			}
		}

		boardMemberRepository.deleteByBoardBoardIdAndUserId(boardId, targetUserId);
	}

	@Override
	public BoardMemberResponse updateBoardMemberRole(int boardId, int targetUserId, String newRole,
			int requestingUserId) {
		findBoardById(boardId);
		requireBoardAdmin(boardId, requestingUserId);

		BoardMember member = boardMemberRepository.findByBoardBoardIdAndUserId(boardId, targetUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Member not found on this board"));

		// Parse the role string safely
		BoardMember.Role role;
		try {
			role = BoardMember.Role.valueOf(newRole.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Invalid role. Must be ADMIN, MEMBER, or OBSERVER");
		}

		// Guard against demoting the last admin
		if (member.getRole() == BoardMember.Role.ADMIN && role != BoardMember.Role.ADMIN) {
			long adminCount = boardMemberRepository.findAllByBoardBoardId(boardId).stream()
					.filter(m -> m.getRole() == BoardMember.Role.ADMIN).count();
			if (adminCount <= 1) {
				throw new BadRequestException("Cannot demote the last admin. Promote another member first.");
			}
		}

		member.setRole(role);
		return BoardMemberResponse.from(boardMemberRepository.save(member));
	}

	@Override
	@Transactional(readOnly = true)
	public BoardAnalyticsResponse getBoardAnalytics(int boardId, int requestingUserId) {
		Board board = findBoardById(boardId);
		requireBoardMembership(boardId, requestingUserId);

		// Count cards per list — note: card counts come from card-service in full impl
		// For now we return list names with 0 counts (card-service not yet built)
		Map<String, Integer> cardCountPerList = new LinkedHashMap<>();
		List<BoardList> activeLists = boardListRepository
				.findAllByBoardBoardIdAndArchivedFalseOrderByPositionAsc(boardId);

		for (BoardList list : activeLists) {
			// TODO: call card-service to get actual card count per list
			cardCountPerList.put(list.getName(), 0);
		}

		return BoardAnalyticsResponse.builder().boardId(boardId).boardName(board.getName())
				.cardCountPerList(cardCountPerList).totalCards(0) // TODO: populate from card-service
				.totalLists(activeLists.size()).totalMembers(board.getMembers().size()).build();
	}

	// =========================================================================
	// LIST (COLUMN) OPERATIONS
	// =========================================================================

	@Override
	public BoardListResponse createList(int boardId, CreateListRequest request, int requestingUserId) {
		Board board = findBoardById(boardId);
		requireBoardOpen(board);
		requireMemberOrAdmin(boardId, requestingUserId);

		/*
		 * Auto-assign position as last + 1. findMaxPositionByBoardId returns 0 if no
		 * lists exist, so the first list gets position 1.
		 */
		int nextPosition = boardListRepository.findMaxPositionByBoardId(boardId) + 1;

		BoardList list = BoardList.builder().board(board).name(request.getName()).position(nextPosition).build();

		return BoardListResponse.from(boardListRepository.save(list));
	}

	@Override
	public BoardListResponse updateList(int boardId, int listId, UpdateListRequest request, int requestingUserId) {
		Board board = findBoardById(boardId);
		requireBoardOpen(board);
		requireMemberOrAdmin(boardId, requestingUserId);

		BoardList list = findListById(listId, boardId);

		if (StringUtils.hasText(request.getName())) {
			list.setName(request.getName());
		}

		return BoardListResponse.from(boardListRepository.save(list));
	}

	@Override
	public BoardListResponse reorderList(int boardId, int listId, ReorderListRequest request, int requestingUserId) {
		Board board = findBoardById(boardId);
		requireBoardOpen(board);
		requireMemberOrAdmin(boardId, requestingUserId);

		BoardList list = findListById(listId, boardId);
		int newPosition = request.getNewPosition();
		int currentPosition = list.getPosition();

		if (newPosition == currentPosition) {
			return BoardListResponse.from(list); // no change needed
		}

		/*
		 * Reordering logic:
		 *
		 * Moving list FROM position 4 TO position 2: → all lists between positions 2
		 * and 3 shift UP by 1 (to make room) → then we set the moved list to position 2
		 *
		 * Moving list FROM position 2 TO position 4: → all lists between positions 3
		 * and 4 shift DOWN by 1 → then we set the moved list to position 4
		 *
		 * shiftPositionsUp handles both cases by shifting lists in the affected range.
		 */
		boardListRepository.shiftPositionsUp(boardId, newPosition, listId);
		list.setPosition(newPosition);

		return BoardListResponse.from(boardListRepository.save(list));
	}

	@Override
	public BoardListResponse archiveList(int boardId, int listId, int requestingUserId) {
		Board board = findBoardById(boardId);
		requireBoardOpen(board);
		requireMemberOrAdmin(boardId, requestingUserId);

		BoardList list = findListById(listId, boardId);

		if (list.isArchived()) {
			throw new BadRequestException("List is already archived");
		}

		list.setArchived(true);
		return BoardListResponse.from(boardListRepository.save(list));
	}

	@Override
	public BoardListResponse unarchiveList(int boardId, int listId, int requestingUserId) {
		findBoardById(boardId);
		requireMemberOrAdmin(boardId, requestingUserId);

		BoardList list = findListById(listId, boardId);

		if (!list.isArchived()) {
			throw new BadRequestException("List is not archived");
		}

		/*
		 * On unarchive, append to the end of active lists. This avoids position
		 * conflicts with currently visible lists.
		 */
		int nextPosition = boardListRepository.findMaxPositionByBoardId(boardId) + 1;
		list.setArchived(false);
		list.setPosition(nextPosition);

		return BoardListResponse.from(boardListRepository.save(list));
	}

	@Override
	public void deleteList(int boardId, int listId, int requestingUserId) {
		findBoardById(boardId);
		requireBoardAdmin(boardId, requestingUserId);

		BoardList list = findListById(listId, boardId);

		// Only allow permanent deletion of archived lists — safety guard
		if (!list.isArchived()) {
			throw new BadRequestException("Archive the list first before permanently deleting it");
		}

		boardListRepository.delete(list);
	}

	@Override
	@Transactional(readOnly = true)
	public List<BoardListResponse> getArchivedLists(int boardId, int requestingUserId) {
		findBoardById(boardId);
		requireBoardMembership(boardId, requestingUserId);

		return boardListRepository.findAllByBoardBoardIdAndArchivedTrueOrderByPositionAsc(boardId).stream()
				.map(BoardListResponse::from).toList();
	}

	// =========================================================================
	// PRIVATE HELPER METHODS
	// =========================================================================

	/**
	 * Fetch a board or throw 404. Centralizing this avoids repeating orElseThrow
	 * everywhere.
	 */
	private Board findBoardById(int boardId) {
		return boardRepository.findById(boardId)
				.orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId));
	}

	/**
	 * Fetch a list that belongs to a specific board, or throw 404. The boardId
	 * check prevents accessing lists from other boards.
	 */
	private BoardList findListById(int listId, int boardId) {
		BoardList list = boardListRepository.findById(listId)
				.orElseThrow(() -> new ResourceNotFoundException("List not found with id: " + listId));

		// Make sure this list actually belongs to the requested board
		if (!list.getBoard().getBoardId().equals(boardId)) {
			throw new ResourceNotFoundException("List does not belong to this board");
		}
		return list;
	}

	/**
	 * Throw 403 if the user is not a member of the board at all.
	 */
	private void requireBoardMembership(int boardId, int userId) {
		if (!boardMemberRepository.existsByBoardBoardIdAndUserId(boardId, userId)) {
			throw new ForbiddenException("You are not a member of this board");
		}
	}

	/**
	 * Throw 403 if the user is not an ADMIN of the board.
	 */
	private void requireBoardAdmin(int boardId, int userId) {
		BoardMember member = boardMemberRepository.findByBoardBoardIdAndUserId(boardId, userId)
				.orElseThrow(() -> new ForbiddenException("You are not a member of this board"));

		if (member.getRole() != BoardMember.Role.ADMIN) {
			throw new ForbiddenException("Only board admins can perform this action");
		}
	}

	/**
	 * Throw 403 if the user is an OBSERVER (read-only). MEMBER and ADMIN can
	 * create/edit cards and lists.
	 */
	private void requireMemberOrAdmin(int boardId, int userId) {
		BoardMember member = boardMemberRepository.findByBoardBoardIdAndUserId(boardId, userId)
				.orElseThrow(() -> new ForbiddenException("You are not a member of this board"));

		if (member.getRole() == BoardMember.Role.OBSERVER) {
			throw new ForbiddenException("Observers cannot make changes to the board");
		}
	}

	/**
	 * Throw 400 if the board is closed (read-only mode). Prevents any write
	 * operations on a closed board.
	 */
	private void requireBoardOpen(Board board) {
		if (board.isClosed()) {
			throw new BadRequestException("This board is closed. Reopen it before making changes.");
		}
	}

	/**
	 * Check if a user can see a board based on its visibility setting.
	 *
	 * PUBLIC → everyone can see WORKSPACE → user must be in the workspace's member
	 * list PRIVATE → user must be a direct board member
	 */
	private boolean canUserSeeBoard(Board board, int userId, List<Integer> workspaceMemberUserIds) {
		return switch (board.getVisibility()) {
		case PUBLIC -> true;
		case WORKSPACE -> workspaceMemberUserIds.isEmpty() || workspaceMemberUserIds.contains(userId);
		case PRIVATE -> boardMemberRepository.existsByBoardBoardIdAndUserId(board.getBoardId(), userId);
		};
	}

	/**
	 * Enforce access when opening a specific board. Throws 403 if the user cannot
	 * see this board.
	 */
	private void checkBoardAccess(Board board, int userId, List<Integer> workspaceMemberUserIds) {
		if (!canUserSeeBoard(board, userId, workspaceMemberUserIds)) {
			throw new ForbiddenException("You do not have access to this board");
		}
	}

	/**
	 * Returns true if user is a board ADMIN.
	 */
	private boolean isAdmin(int boardId, int userId) {
		return boardMemberRepository.findByBoardBoardIdAndUserId(boardId, userId)
				.map(m -> m.getRole() == BoardMember.Role.ADMIN).orElse(false);
	}
}