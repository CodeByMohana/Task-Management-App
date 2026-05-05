package com.app.taskmanagement.label.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A Label is a colour-coded tag that can be attached to cards.
 *
 * HOW LABELS WORK IN FLOWBOARD:
 *   - Labels belong to a BOARD, not to a card directly.
 *     Example: Board "Sprint 1" has labels: "Bug" (red), "Feature" (green), "Urgent" (orange).
 *   - Any card on that board can use any of the board's labels.
 *   - The link between a label and a card is stored in the CardLabel join table (see CardLabel.java).
 *
 * WHY BOARD-SCOPED LABELS?
 *   - Teams often reuse the same label names across cards (e.g. "Bug" appears on 20 cards).
 *   - Storing the label once per board and referencing it from cards avoids storing
 *     "Bug" 20 times with 20 different hex color values.
 *   - If someone renames the "Bug" label to "Defect", it updates everywhere at once.
 *
 * CROSS-SERVICE NOTE:
 *   boardId is stored as a plain integer. Board data lives in board-service's database.
 *   Microservices never share tables — they reference other services' data by ID only.
 */
@Entity
@Table(name = "labels", indexes = {
		// Most common query: "get all labels for this board" — index speeds this up
		@Index(name = "idx_label_board_id", columnList = "board_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer labelId;

	/**
	 * The board this label belongs to. Example: boardId = 42 means this label can
	 * only be used on board 42. Stored as a plain integer (not @ManyToOne) because
	 * board data is in a different database.
	 */
	@Column(name = "board_id", nullable = false)
	private Integer boardId;

	/**
	 * Human-readable name for the label. Example: "Bug", "Feature", "Urgent",
	 * "Needs Review"
	 */
	@Column(nullable = false, length = 50)
	private String name;

	/**
	 * The display color as a hex code. Example: "#FF5733" (red-orange), "#28A745"
	 * (green), "#FFC107" (yellow) The frontend uses this to render the colored
	 * label chip on cards.
	 */
	@Column(nullable = false, length = 7)
	private String color;

	// Auto-set when the label is first saved to the database
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}