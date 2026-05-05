package com.app.taskmanagement.boardservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for renaming a list.
 */
@Data
public class UpdateListRequest {

	@Size(min = 1, max = 100)
	private String name;
}