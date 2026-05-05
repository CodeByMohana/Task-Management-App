package com.app.taskmanagement.label.repository;

import com.app.taskmanagement.label.entity.CardLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Database access for the CardLabel join table.
 *
 * This repository handles the many-to-many relationship between cards and
 * labels. Think of it as managing a connection: "card 10 has labels 3, 5, and
 * 7".
 */
public interface CardLabelRepository extends JpaRepository<CardLabel, Integer> {

	/**
	 * Get all labels attached to a specific card.
	 *
	 * "findAllByCardId" → WHERE card_id = ? The result includes the full Label
	 * object because @ManyToOne eager/lazy loading will fetch it when we access it
	 * in the response builder.
	 *
	 * Example: card 10 has labels 3 ("Bug") and 5 ("Urgent") → returns both
	 * CardLabel rows.
	 */
	List<CardLabel> findAllByCardId(int cardId);

	/**
	 * Find a specific card-label link. Used to check "does card 10 already have
	 * label 3?" before adding it again.
	 *
	 * The label is fetched via the @ManyToOne join — returns the full Label object
	 * inside the CardLabel, so we can validate things like boardId.
	 */
	Optional<CardLabel> findByCardIdAndLabelLabelId(int cardId, int labelId);

	/**
	 * Remove a label from a card.
	 *
	 * @Modifying marks this as a write operation (INSERT/UPDATE/DELETE), not a
	 *            SELECT. Without @Modifying, Spring will throw an error if you try
	 *            to run a DELETE query.
	 *
	 * @Query lets us write our own JPQL (Java Persistence Query Language). JPQL
	 *        looks like SQL but uses Java class/field names instead of table/column
	 *        names. "CardLabel c" → the CardLabel entity class "c.cardId" → the
	 *        cardId field on CardLabel "c.label.labelId" → navigate the @ManyToOne
	 *        relationship to get the label's ID
	 */
	@Modifying
	@Query("DELETE FROM CardLabel c WHERE c.cardId = :cardId AND c.label.labelId = :labelId")
	void deleteByCardIdAndLabelId(@Param("cardId") int cardId, @Param("labelId") int labelId);

	/**
	 * Remove ALL labels from a card at once. Called when a card is deleted — cleans
	 * up all its label links.
	 */
	@Modifying
	@Query("DELETE FROM CardLabel c WHERE c.cardId = :cardId")
	void deleteAllByCardId(@Param("cardId") int cardId);

	/**
	 * Check if a card already has a specific label. Returns true/false quickly
	 * without loading the full object. Used before adding a label to avoid the
	 * duplicate error from the unique constraint.
	 */
	boolean existsByCardIdAndLabelLabelId(int cardId, int labelId);
}