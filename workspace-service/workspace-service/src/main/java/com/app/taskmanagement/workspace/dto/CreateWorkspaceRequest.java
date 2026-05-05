package com.app.taskmanagement.workspace.dto;


import com.app.taskmanagement.workspace.entity.Workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data sent by the client when creating a new workspace.
 * We use a separate DTO (not the entity) so the API input is independent
 * from the database model — a good practice to avoid over-posting attacks.
 */
@Data
public class CreateWorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    private String description;

    /**
     * Defaults to PRIVATE if not specified.
     * Client sends "PUBLIC" or "PRIVATE" as a string.
     */
    private Workspace.Visibility visibility = Workspace.Visibility.PRIVATE;
}