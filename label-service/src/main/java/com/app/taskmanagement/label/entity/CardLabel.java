package com.app.taskmanagement.label.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * The link between a Label and a Card.
 *
 * This is a JOIN TABLE — it exists purely to say "card X has label Y".
 *
 * WHY A SEPARATE TABLE? A card can have MANY labels, and a label can be on MANY
 * cards. This is a many-to-many relationship, which databases handle with a
 * join table.
 *
 * Example data in the cardlabels table: | cardLabelId | cardId | labelId |
 * |-------------|--------|---------| | 1 | 10 | 3 | ← card 10 has label 3
 * ("Bug") | 2 | 10 | 5 | ← card 10 also has label 5 ("Urgent") | 3 | 15 | 3 | ←
 * card 15 also has label 3 ("Bug")
 *
 * WHY NOT @ManyToMany DIRECTLY ON LABEL?
 * 
 * @ManyToMany with a join table works but makes it hard to query "which cards
 *             have this label?" or add extra fields to the relationship. An
 *             explicit entity gives us full control and is easier to
 *             understand.
 *
 *             CROSS-SERVICE NOTE: cardId lives in card-service's database. We
 *             store it as a plain integer. labelId links to the Label entity in
 *             THIS database (same service, same DB).
 */
@Entity
@Table(name = "card_labels", indexes = {
		// "Get all labels for card X" — most common query on this table
		@Index(name = "idx_card_label_card_id", columnList = "card_id"),
		// "Get all cards that have label Y" — used for label-based filtering
		@Index(name = "idx_card_label_label_id", columnList = "label_id") },
		// Prevent the same label being added to the same card twice
		uniqueConstraints = @UniqueConstraint(name = "uq_card_label", columnNames = { "card_id", "label_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardLabel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer cardLabelId;

	/**
	 * The card this label is attached to. Stored as a plain integer — card data
	 * lives in card-service's database.
	 */
	@Column(name = "card_id", nullable = false)
	private Integer cardId;

	/**
	 * The label being applied to the card.
	 *
	 * @ManyToOne means many CardLabel rows can reference the same Label.
	 *            FetchType.LAZY means we don't load the Label until we actually
	 *            need it (avoids unnecessary database queries).
	 */

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "label_id", nullable = false)
	private Label label;

	// When this label was added to the card — useful for audit purposes
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime addedAt;
}