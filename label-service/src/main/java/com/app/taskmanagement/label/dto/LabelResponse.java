package com.app.taskmanagement.label.dto;

import com.app.taskmanagement.label.entity.Label;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * What we send back to the client when they ask for a label.
 *
 * We NEVER send the raw Label entity directly. Reasons: 1. Entities may contain
 * fields we don't want to expose (e.g. internal state). 2. Decouples our API
 * contract from the database structure. If we rename a database column, the API
 * response stays the same. 3. Makes it clear exactly what the client will
 * receive.
 */
@Getter
@Builder
public class LabelResponse {

	private Integer labelId;
	private Integer boardId;
	private String name;

	// Hex color code — frontend uses this to render the colored label chip
	private String color;

	private LocalDateTime createdAt;

	/**
	 * Factory method: converts a Label entity into a LabelResponse. Called in the
	 * service layer before returning data to the controller.
	 *
	 * Example: Label entity { labelId=3, boardId=5, name="Bug", color="#FF5733" } →
	 * LabelResponse { labelId=3, boardId=5, name="Bug", color="#FF5733" }
	 */
	public static LabelResponse from(Label label) {
		return LabelResponse.builder().labelId(label.getLabelId()).boardId(label.getBoardId()).name(label.getName())
				.color(label.getColor()).createdAt(label.getCreatedAt()).build();
	}
}