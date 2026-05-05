package com.app.taskmanagement.boardservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for creating a new list (column) on a board. Position is
 * auto-assigned by the service (appended at the end).
 */
@Data
public class CreateListRequest {

	@NotBlank(message = "List name is required")
	@Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
	private String name;
}