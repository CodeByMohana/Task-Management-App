package com.app.taskmanagement.boardservice.dto;

import com.app.taskmanagement.boardservice.entity.Board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request body sent by client when creating a new board.
 *
 * The workspaceId tells us which workspace this board belongs to. The
 * requesting user (from JWT) becomes the board creator and first ADMIN.
 */
@Data
public class CreateBoardRequest {

	@NotNull(message = "Workspace ID is required")
	private Integer workspaceId;

	@NotBlank(message = "Board name is required")
	@Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
	private String name;

	private String description;

	/**
	 * Hex color or image URL for the board background. Example: "#0052CC" or
	 * "https://cdn.example.com/bg.jpg"
	 */
	private String background;

	/**
	 * Defaults to WORKSPACE — visible to all workspace members.
	 */
	private Board.Visibility visibility = Board.Visibility.WORKSPACE;

	/**
	 * Optional list of workspace member user IDs to auto-add as board MEMBER.
	 * The creator is always added as ADMIN regardless of this list.
	 */
	private List<Integer> workspaceMemberIds;
}

