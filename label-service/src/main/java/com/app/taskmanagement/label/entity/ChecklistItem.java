package com.app.taskmanagement.label.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single to-do item inside a Checklist.
 *
 * HOW ITEMS WORK: Example checklist "Backend tasks" might have items: [ ] Write
 * unit tests (assigneeId = null, no due date) [✓] Set up Docker (completed =
 * true) [ ] Deploy to staging (assigneeId = 5 = "Alice", dueDate = 2026-05-01)
 *
 * POSITION: Controls the order items are shown inside the checklist. When items
 * are reordered (drag-and-drop), we update these position numbers.
 *
 * OPTIONAL ASSIGNEE & DUE DATE: Items can optionally be assigned to a specific
 * person and given their own due date. These are separate from the card's own
 * assignee and due date. The assigneeId is a userId from auth-service.
 */
@Entity
@Table(name = "checklist_items", indexes = {
		// Most common query: "get all items for checklist X"
		@Index(name = "idx_item_checklist_id", columnList = "checklist_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer itemId;

	/**
	 * The checklist this item belongs to.
	 *
	 * @ManyToOne — many items can belong to one checklist. FetchType.LAZY — don't
	 *            load the full Checklist object unless we need it.
	 * @JoinColumn — the "checklist_id" column in the checklist_items table.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "checklist_id", nullable = false)
	private Checklist checklist;

	/**
	 * The task text for this item. Example: "Write unit tests", "Review PR #42",
	 * "Update documentation"
	 */
	@Column(nullable = false, length = 500)
	private String text;

	/**
	 * Has this item been ticked off as done? true = done (shown with a
	 * strikethrough in the UI) false = not done (default when item is first
	 * created)
	 */
	@Column(nullable = false)
	@Builder.Default
	private boolean completed = false;

	/**
	 * Display order within the checklist (1 = first, 2 = second, etc.) Allows items
	 * to be reordered by drag-and-drop.
	 */
	@Column(nullable = false)
	@Builder.Default
	private Integer position = 1;

	/**
	 * Optional: the userId of a specific team member responsible for this item.
	 * Null means "no one assigned specifically" (the card's assignee is
	 * responsible). Stored as a plain integer — user data lives in auth-service's
	 * database.
	 */
	private Integer assigneeUserId;

	/**
	 * Optional: when this specific item needs to be done by. This is separate from
	 * the card's overall due date. LocalDate = date only (no time component
	 * needed).
	 */
	private LocalDate dueDate;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}