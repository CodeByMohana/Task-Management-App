package com.app.taskmanagement.label.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Checklist is a sub-task list that lives inside a card.
 *
 * HOW CHECKLISTS WORK: - A card can have multiple checklists. Example: Card
 * "Set up CI/CD" might have: Checklist 1: "Backend" → items: [Install Jenkins
 * ✓, Configure pipeline □, Add tests □] Checklist 2: "Frontend" → items: [Add
 * lint checks □, Configure Vercel □] - Each checklist has a title and an
 * ordered list of ChecklistItems. - The progress bar on the card (e.g. "2/5
 * done") is calculated from the items.
 *
 * WHY SEPARATE FROM CARD? - Card data lives in card-service's database. -
 * Checklist data lives here in label-service's database. - We reference the
 * card by its ID (cardId) but don't share a database table.
 *
 * POSITION FIELD: - Controls the display order when a card has multiple
 * checklists. - Lower number = shown first. - When a checklist is created,
 * position is auto-set to (max existing + 1).
 */
@Entity
@Table(name = "checklists", indexes = {
		// Most common query: "get all checklists for card X"
		@Index(name = "idx_checklist_card_id", columnList = "card_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checklist {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer checklistId;

	/**
	 * The card this checklist belongs to. Plain integer — card data is in
	 * card-service's database (different microservice).
	 */
	@Column(name = "card_id", nullable = false)
	private Integer cardId;

	/**
	 * The title of this checklist. Example: "Backend tasks", "Frontend tasks",
	 * "Definition of Done"
	 */
	@Column(nullable = false, length = 100)
	private String title;

	/**
	 * Display order when a card has multiple checklists. Checklist with position=1
	 * appears above position=2, etc.
	 */
	@Column(nullable = false)
	@Builder.Default
	private Integer position = 1;

	/**
	 * The sub-task items inside this checklist.
	 *
	 * CascadeType.ALL means: - When we save a checklist, its items are saved
	 * automatically. - When we DELETE a checklist, its items are also deleted
	 * automatically. (No orphaned items left in the database.)
	 *
	 * orphanRemoval = true means: - If we remove an item from this list in Java, it
	 * gets deleted from the DB too.
	 *
	 * mappedBy = "checklist" means the ChecklistItem entity owns the foreign key
	 * (checklist_id column).
	 */
	@OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Builder.Default
	@OrderBy("position ASC") // Always load items in the correct order
	private List<ChecklistItem> items = new ArrayList<>();

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}