package com.app.taskmanagement.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for creating OR updating a label on a board.
 *
 * Example JSON body: { "name": "Bug", "color": "#FF5733" }
 *
 * This same DTO is reused for both POST (create) and PUT (update) requests
 * because both operations need the same two fields.
 */
@Getter
@Setter
@NoArgsConstructor
public class LabelRequest {

	@NotBlank(message = "Label name must not be blank")
	@Size(max = 50, message = "Label name must not exceed 50 characters")
	private String name;

	/**
	 * Hex color code for the label's background color. The regex
	 * "^#[0-9A-Fa-f]{6}$" means: ^ = start of string # = literal hash symbol
	 * [0-9A-Fa-f] = any hex digit (0-9, A-F, a-f) {6} = exactly 6 of them $ = end
	 * of string
	 *
	 * Valid: "#FF5733", "#28a745", "#000000" Invalid: "red", "#FFF" (shorthand),
	 * "FF5733" (no hash)
	 */
	@NotBlank(message = "Color must not be blank")
	@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code like #FF5733")
	private String color;
}