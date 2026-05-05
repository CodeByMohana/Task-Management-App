package com.app.taskmanagement.workspace.dto;


import com.app.taskmanagement.workspace.entity.Workspace;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data sent by the client when updating an existing workspace. All fields are
 * optional — only non-null fields will be updated (PATCH-style logic).
 */
@Data
public class UpdateWorkspaceRequest {

	@Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
	private String name;

	private String description;

	private Workspace.Visibility visibility;
}