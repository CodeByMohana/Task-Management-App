package com.app.taskmanagement.workspace.dto;


import com.app.taskmanagement.workspace.entity.WorkspaceMember;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data sent when adding a new member to a workspace.
 * The client provides the userId of the user to add and their initial role.
 */
@Data
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Integer userId;

    /**
     * Role defaults to MEMBER if not provided.
     */
    private WorkspaceMember.Role role = WorkspaceMember.Role.MEMBER;
}