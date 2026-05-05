package com.app.taskmanagement.label.service;

import com.app.taskmanagement.label.dto.*;

import java.util.List;

/**
 * Defines all the operations label-service can perform.
 *
 * WHY AN INTERFACE?
 *   The controller depends on LabelService (the interface), not LabelServiceImpl (the class).
 *   This means:
 *   - We can swap implementations without changing the controller (e.g. for testing).
 *   - It's a contract: "here are ALL the things this service can do."
 *   - Consistent with the pattern used across all FlowBoard microservices.
 */
public interface LabelService {

    // ── Label Operations ──────────────────────────────────────────────────────

    /** Create a new label (e.g. "Bug" in red) on a board. */
    LabelResponse createLabel(int boardId, LabelRequest request);

    /** Get all labels available on a board, so users can pick which to apply to cards. */
    List<LabelResponse> getLabelsByBoard(int boardId);

    /** Update a label's name or color. */
    LabelResponse updateLabel(int boardId, int labelId, LabelRequest request);

    /** Permanently delete a label from a board. Also removes it from all cards on that board. */
    void deleteLabel(int boardId, int labelId);

    // ── Card-Label Association ────────────────────────────────────────────────

    /** Attach a label to a card. Example: apply "Bug" label to card 10. */
    void addLabelToCard(int cardId, int labelId);

    /** Remove a label from a card. Example: remove "Bug" from card 10. */
    void removeLabelFromCard(int cardId, int labelId);

    /** Get all labels currently on a card. */
    List<LabelResponse> getLabelsForCard(int cardId);

    // ── Checklist Operations ──────────────────────────────────────────────────

    /** Create a new checklist on a card. Example: "Backend tasks" on card 10. */
    ChecklistResponse createChecklist(int cardId, CreateChecklistRequest request);

    /** Get all checklists for a card, each with their items and progress. */
    List<ChecklistResponse> getChecklistsByCard(int cardId);

    /** Delete a checklist and ALL its items. */
    void deleteChecklist(int checklistId);

    // ── Checklist Item Operations ─────────────────────────────────────────────

    /** Add a new item to the bottom of a checklist. */
    ChecklistItemResponse addItem(int checklistId, AddChecklistItemRequest request);

    /**
     * Toggle a checklist item between done (✓) and not done (□).
     * If the item is done → mark as not done. If not done → mark as done.
     * Returns the updated item.
     */
    ChecklistItemResponse toggleItem(int itemId);

    /** Permanently delete a single checklist item. */
    void deleteItem(int itemId);

    /**
     * Get the progress of a specific checklist.
     * Returns: how many items are done, total items, and percentage.
     */
    ChecklistResponse getChecklistProgress(int checklistId);
}