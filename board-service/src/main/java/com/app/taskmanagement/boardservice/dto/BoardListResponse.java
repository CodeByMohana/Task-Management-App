package com.app.taskmanagement.boardservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.app.taskmanagement.boardservice.entity.BoardList;

/**
 * Represents one list (column) in the board response. Cards within the list are
 * handled by card-service separately.
 */
@Data
@Builder
public class BoardListResponse {

	private Integer listId;
	private Integer boardId;
	private String name;
	private Integer position;
	private boolean archived;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public static BoardListResponse from(BoardList list) {
		return BoardListResponse.builder().listId(list.getListId()).boardId(list.getBoard().getBoardId())
				.name(list.getName()).position(list.getPosition()).archived(list.isArchived())
				.createdAt(list.getCreatedAt()).updatedAt(list.getUpdatedAt()).build();
	}
}