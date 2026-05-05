package com.app.taskmanagement.boardservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.app.taskmanagement.boardservice.entity.Board;

/**
 * What the API returns to the client for board data.
 *
 * We never return the raw entity — always use a DTO. This lets us control
 * exactly what's exposed and add computed fields (like memberCount) without
 * adding them to the entity.
 */
@Data
@Builder
public class BoardResponse {

	private Integer boardId;
	private Integer workspaceId;
	private String name;
	private String description;
	private String background;
	private String visibility;
	private boolean closed;
	private Integer createdByUserId;
	private int memberCount;
	private int listCount;

	/**
	 * Full list of members — only included when fetching a single board. Omitted
	 * when listing many boards to keep responses lightweight.
	 */
	private List<BoardMemberResponse> members;

	/**
	 * All active lists on this board — included when opening a board.
	 */
	private List<BoardListResponse> lists;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	/**
	 * Convert Board entity → BoardResponse DTO.
	 *
	 * @param board          the entity from DB
	 * @param includeDetails true = include full members + lists (single board view)
	 *                       false = lightweight summary (board list view)
	 */
	public static BoardResponse from(Board board, boolean includeDetails) {
		return BoardResponse.builder().boardId(board.getBoardId()).workspaceId(board.getWorkspaceId())
				.name(board.getName()).description(board.getDescription()).background(board.getBackground())
				.visibility(board.getVisibility().name()).closed(board.isClosed())
				.createdByUserId(board.getCreatedByUserId()).memberCount(board.getMembers().size())
				.listCount(board.getLists().size())
				.members(includeDetails ? board.getMembers().stream().map(BoardMemberResponse::from).toList() : null)
				.lists(includeDetails ? board.getLists().stream().filter(l -> !l.isArchived()) // only show active lists
						.map(BoardListResponse::from).toList() : null)
				.createdAt(board.getCreatedAt()).updatedAt(board.getUpdatedAt()).build();
	}
}