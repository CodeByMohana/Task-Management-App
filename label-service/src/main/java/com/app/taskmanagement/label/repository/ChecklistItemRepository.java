package com.app.taskmanagement.label.repository;

import com.app.taskmanagement.label.entity.ChecklistItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database access for ChecklistItem entities.
 */
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Integer> {

	/**
	 * Find the highest position number among all items in a checklist. Used to
	 * place new items at the bottom of the list automatically.
	 *
	 * Example: checklist has items at positions 1, 2, 3 → returns 3. New item gets
	 * position 4 (appended to the bottom).
	 *
	 * COALESCE: returns 0 if the checklist has no items yet, so first item gets
	 * position 1.
	 */
	@Query("SELECT COALESCE(MAX(i.position), 0) FROM ChecklistItem i WHERE i.checklist.checklistId = :checklistId")
	int findMaxPositionByChecklistId(@Param("checklistId") int checklistId);

	/**
	 * Count how many items in a checklist are marked as completed. Used together
	 * with countByChecklistChecklistId() to calculate progress percentage.
	 *
	 * Example: 3 completed out of 5 total → "3/5" or "60%" progress shown on the
	 * card.
	 */
	int countByChecklistChecklistIdAndCompletedTrue(int checklistId);

	/**
	 * Count the total number of items in a checklist (completed + incomplete). Used
	 * with countByChecklistChecklistIdAndCompletedTrue() to show progress.
	 */
	int countByChecklistChecklistId(int checklistId);
}