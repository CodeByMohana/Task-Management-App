package com.app.taskmanagement.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for creating a new checklist on a card. Example JSON: { "title":
 * "Backend tasks" }
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateChecklistRequest {

	@NotBlank(message = "Checklist title must not be blank")
	@Size(max = 100, message = "Checklist title must not exceed 100 characters")
	private String title;
}