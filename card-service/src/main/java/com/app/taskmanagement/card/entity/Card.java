package com.app.taskmanagement.card.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Card — the primary task unit in FlowBoard.
 *
 * Hierarchy: Workspace → Board → List → Card
 *
 * A card lives inside a list (column) on a board. Example: card "Fix login bug"
 * in list "In Progress" on board "Sprint 1".
 *
 * Key design decisions: - listId and boardId are stored as plain integers, NOT
 * as @ManyToOne references. This is because lists and boards live in
 * board-service's DB, not this one. Microservices never share tables — they
 * reference by ID only.
 *
 * - position controls the top-to-bottom order within a list. When a user drags
 * a card, we update this integer.
 *
 * - archived = soft delete. Archived cards are hidden but recoverable.
 */
@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer cardId;

	/**
	 * The list (column) this card belongs to. Stored as plain ID — list data lives
	 * in board-service DB. When a card is moved to another list, only this field
	 * changes.
	 */
	@Column(nullable = false)
	private Integer listId;

	/**
	 * The board this card belongs to. Stored for quick lookups — avoids calling
	 * board-service for every card query.
	 */
	@Column(nullable = false)
	private Integer boardId;

	/**
	 * The workspace this card belongs to. Stored for cross-workspace search
	 * functionality.
	 */
	@Column(nullable = false)
	private Integer workspaceId;

	/**
	 * Short title of the card. Example: "Fix login bug", "Design landing page",
	 * "Write unit tests"
	 */
	@Column(nullable = false)
	private String title;

	/**
	 * Rich text description of the card. Can contain markdown or HTML (handled by
	 * frontend). columnDefinition = TEXT allows storing longer content than
	 * VARCHAR(255).
	 */
	@Column(columnDefinition = "TEXT")
	private String description;

	/**
	 * Task urgency level. LOW → not urgent, do it eventually MEDIUM → normal
	 * priority HIGH → should be done soon CRITICAL → blocking, do it now
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Priority priority = Priority.MEDIUM;

	/**
	 * Current workflow stage of the card. TO_DO → not started IN_PROGRESS → being
	 * worked on IN_REVIEW → waiting for review/approval DONE → completed
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Status status = Status.TO_DO;

	/**
	 * The userId of the person assigned to work on this card. Nullable — a card can
	 * be unassigned. Stored as ID — user data lives in auth-service DB.
	 */
	private Integer assigneeUserId;

	/**
	 * The userId who created this card.
	 */
	@Column(nullable = false)
	private Integer createdByUserId;

	/**
	 * The date by which this card should be completed. LocalDate = date only (no
	 * time component needed for due dates).
	 */
	private LocalDate dueDate;

	/**
	 * When work on this card should begin.
	 */
	private LocalDate startDate;

	/**
	 * Background color for the card's visual cover in the UI. Example: "#FF5733",
	 * "#00B8D9"
	 */
	private String coverColor;

	/**
	 * Position of this card within its list (top-to-bottom order). Lower number =
	 * higher up in the list. When reordering, we update position values of affected
	 * cards.
	 */
	@Column(nullable = false)
	private Integer position;

	/**
	 * Soft-delete flag. Archived cards are hidden from the board but kept in DB for
	 * recovery.
	 */
	@Column(nullable = false)
	@Builder.Default
	private boolean archived = false;

	/**
	 * File attachments uploaded to this card. CascadeType.ALL — deleting a card
	 * also deletes its attachment records. orphanRemoval = true — removing from
	 * list deletes the DB record.
	 */
	@OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<CardAttachment> attachments = new ArrayList<>();

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	// ─── Enums ───────────────────────────────────────────────────────────────

	public enum Priority {
		LOW, MEDIUM, HIGH, CRITICAL
	}

	public enum Status {
		TO_DO, IN_PROGRESS, IN_REVIEW, DONE
	}
}