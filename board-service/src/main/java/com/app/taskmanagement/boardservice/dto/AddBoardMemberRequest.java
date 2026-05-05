package com.app.taskmanagement.boardservice.dto;

import com.app.taskmanagement.boardservice.entity.BoardMember;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for adding a user to a board.
 */
@Data
public class AddBoardMemberRequest {

    @NotNull(message = "User ID is required")
    private Integer userId;

    /**
     * Defaults to MEMBER if not specified.
     */
    private BoardMember.Role role = BoardMember.Role.MEMBER;
}
