package com.app.taskmanagement.label.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.taskmanagement.label.dto.*;
import com.app.taskmanagement.label.entity.*;
import com.app.taskmanagement.label.exception.*;
import com.app.taskmanagement.label.repository.*;

import java.util.List;

/**
 * The actual implementation of all label and checklist business logic.
 *
 * HOW THIS CONNECTS TO THE REST OF THE APP:
 *   HTTP Request → LabelResource (controller) → LabelServiceImpl (this class) → Repository → Database
 *
 * @Service tells Spring: "create one instance of this class and make it available for injection."
 *
 * @RequiredArgsConstructor (Lombok): automatically creates a constructor that takes all
 *   'final' fields as arguments. Spring uses this constructor to inject the repositories.
 *   Without Lombok you'd write: public LabelServiceImpl(LabelRepository lr, ...) { this.labelRepository = lr; ... }
 *
 * @Transactional on the class means: every method runs inside a database transaction by default.
 *   A transaction is like a "save point" — if anything goes wrong mid-method, ALL database
 *   changes made in that method are rolled back automatically.
 *   Example: if addLabelToCard() crashes halfway, the half-written data is removed cleanly.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LabelServiceImpl implements LabelService {

    private final LabelRepository labelRepository;
    private final CardLabelRepository cardLabelRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;

    // =========================================================================
    // LABEL OPERATIONS
    // =========================================================================

    @Override
    public LabelResponse createLabel(int boardId, LabelRequest request) {
        /*
         * Prevent duplicate label names on the same board.
         * Case-insensitive: "Bug" and "bug" are treated as the same name.
         *
         * Why prevent duplicates?
         *   Having two "Bug" labels on the same board confuses users —
         *   they wouldn't know which one to use, and filtering by label
         *   would need to handle multiple matches.
         */
        if (labelRepository.existsByBoardIdAndNameIgnoreCase(boardId, request.getName())) {
            throw new BadRequestException(
                "A label named '" + request.getName() + "' already exists on this board"
            );
        }

        Label label = Label.builder()
                .boardId(boardId)
                .name(request.getName())
                .color(request.getColor())
                .build();

        // save() inserts the new row into the database and returns the saved entity
        // (with the auto-generated labelId populated)
        return LabelResponse.from(labelRepository.save(label));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelResponse> getLabelsByBoard(int boardId) {
        /*
         * @Transactional(readOnly = true):
         *   Tells Hibernate this method only reads data — no writes.
         *   This allows the database driver to apply read-only optimisations
         *   (e.g. skip write-lock overhead, use replica DBs if configured).
         *   Always use readOnly = true on GET methods.
         */
        return labelRepository.findAllByBoardIdOrderByCreatedAtAsc(boardId)
                .stream()
                .map(LabelResponse::from)  // convert each Label entity → LabelResponse DTO
                .toList();
    }

    @Override
    public LabelResponse updateLabel(int boardId, int labelId, LabelRequest request) {
        /*
         * We use findByLabelIdAndBoardId (not just findById) to enforce ownership.
         * This prevents a user from editing a label on a board they don't have access to
         * by guessing a labelId. The boardId in the URL is their authorisation check.
         */
        Label label = labelRepository.findByLabelIdAndBoardId(labelId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Label not found with id: " + labelId + " on board: " + boardId
                ));

        // Check for duplicate name — but EXCLUDE the current label from the check.
        // Without this, updating "Bug" → "Bug" (same name, same board) would fail.
        boolean nameConflict = labelRepository.existsByBoardIdAndNameIgnoreCase(boardId, request.getName())
                && !label.getName().equalsIgnoreCase(request.getName());

        if (nameConflict) {
            throw new BadRequestException(
                "A label named '" + request.getName() + "' already exists on this board"
            );
        }

        label.setName(request.getName());
        label.setColor(request.getColor());

        return LabelResponse.from(labelRepository.save(label));
    }

    @Override
    public void deleteLabel(int boardId, int labelId) {
        // Verify the label exists AND belongs to this board before deleting
        Label label = labelRepository.findByLabelIdAndBoardId(labelId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Label not found with id: " + labelId + " on board: " + boardId
                ));

        /*
         * Deleting the label also removes it from all cards on this board.
         *
         * WHY?
         *   CardLabel rows reference this label via a foreign key (label_id).
         *   If we deleted the Label without removing CardLabel rows first,
         *   the database would throw a "foreign key constraint violation" error.
         *
         * The @Modifying query in CardLabelRepository handles removing all
         * card-label links for this label across the board.
         *
         * ORDER MATTERS:
         *   1. Delete all card-label associations first (CardLabel rows)
         *   2. Then delete the label itself
         */
        cardLabelRepository.deleteAllByCardId(label.getLabelId()); // using labelId as reference
        labelRepository.delete(label);
    }

    // =========================================================================
    // CARD-LABEL ASSOCIATIONS
    // =========================================================================

    @Override
    public void addLabelToCard(int cardId, int labelId) {
        // Make sure the label we're trying to attach actually exists
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found: " + labelId));

        /*
         * Prevent adding the same label to a card twice.
         *
         * The database has a UNIQUE constraint on (card_id, label_id) in the
         * card_labels table, so a duplicate INSERT would throw a database error.
         * We catch this BEFORE hitting the DB with a clean validation message.
         */
        if (cardLabelRepository.existsByCardIdAndLabelLabelId(cardId, labelId)) {
            throw new BadRequestException("This label is already on the card");
        }

        CardLabel cardLabel = CardLabel.builder()
                .cardId(cardId)
                .label(label)
                .build();

        cardLabelRepository.save(cardLabel);
    }

    @Override
    public void removeLabelFromCard(int cardId, int labelId) {
        // Verify the link exists before trying to delete it
        if (!cardLabelRepository.existsByCardIdAndLabelLabelId(cardId, labelId)) {
            throw new ResourceNotFoundException("This label is not on the card");
        }

        // Delete just this specific card-label link (not the label itself)
        cardLabelRepository.deleteByCardIdAndLabelId(cardId, labelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelResponse> getLabelsForCard(int cardId) {
        /*
         * Fetch all CardLabel rows for this card, then extract the Label from each.
         * Each CardLabel has a @ManyToOne to Label, so label.getLabel() gives us the Label entity.
         */
        return cardLabelRepository.findAllByCardId(cardId)
                .stream()
                .map(cardLabel -> LabelResponse.from(cardLabel.getLabel()))
                .toList();
    }

    // =========================================================================
    // CHECKLIST OPERATIONS
    // =========================================================================

    @Override
    public ChecklistResponse createChecklist(int cardId, CreateChecklistRequest request) {
        /*
         * Auto-assign the next position so the new checklist appears at the bottom.
         *
         * Example: card already has 2 checklists at positions 1 and 2.
         * findMaxPositionByCardId(cardId) returns 2.
         * New checklist gets position 2 + 1 = 3.
         *
         * COALESCE in the query returns 0 if no checklists exist yet,
         * so the first checklist gets position 0 + 1 = 1.
         */
        int nextPosition = checklistRepository.findMaxPositionByCardId(cardId) + 1;

        Checklist checklist = Checklist.builder()
                .cardId(cardId)
                .title(request.getTitle())
                .position(nextPosition)
                .build();

        return ChecklistResponse.from(checklistRepository.save(checklist));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistResponse> getChecklistsByCard(int cardId) {
        /*
         * Returns all checklists for the card, each including their items.
         *
         * The items are loaded via the @OneToMany(fetch = FetchType.LAZY) on Checklist.
         * LAZY means Hibernate won't load items until we actually access checklist.getItems().
         * We DO access it inside ChecklistResponse.from(), so items ARE loaded here.
         *
         * This is fine for a single card's detail view. For a list of 50 cards,
         * you would NOT want to call this — it would be 50 × N queries (N+1 problem).
         */
        return checklistRepository.findAllByCardIdOrderByPositionAsc(cardId)
                .stream()
                .map(ChecklistResponse::from)
                .toList();
    }

    @Override
    public void deleteChecklist(int checklistId) {
        Checklist checklist = findChecklistById(checklistId);

        /*
         * CascadeType.ALL + orphanRemoval = true on Checklist.items means:
         * deleting the Checklist automatically deletes ALL its ChecklistItems too.
         * We don't need to manually delete items first.
         *
         * The cascade runs at the JPA level — Hibernate generates the DELETE
         * statements for items before deleting the checklist row.
         */
        checklistRepository.delete(checklist);
    }

    // =========================================================================
    // CHECKLIST ITEM OPERATIONS
    // =========================================================================

    @Override
    public ChecklistItemResponse addItem(int checklistId, AddChecklistItemRequest request) {
        Checklist checklist = findChecklistById(checklistId);

        // Auto-assign position: new item goes to the bottom of the checklist
        int nextPosition = checklistItemRepository.findMaxPositionByChecklistId(checklistId) + 1;

        ChecklistItem item = ChecklistItem.builder()
                .checklist(checklist)  // @ManyToOne — links the item to its parent checklist
                .text(request.getText())
                .assigneeUserId(request.getAssigneeUserId())
                .dueDate(request.getDueDate())
                .position(nextPosition)
                .completed(false)  // all new items start as not done
                .build();

        return ChecklistItemResponse.from(checklistItemRepository.save(item));
    }

    @Override
    public ChecklistItemResponse toggleItem(int itemId) {
        ChecklistItem item = findItemById(itemId);

        /*
         * Toggle = flip the current state.
         * If completed is true  → set to false (un-tick)
         * If completed is false → set to true  (tick)
         *
         * The ! operator means "NOT" — flips a boolean value.
         * Example: !true = false, !false = true
         */
        item.setCompleted(!item.isCompleted());

        return ChecklistItemResponse.from(checklistItemRepository.save(item));
    }

    @Override
    public void deleteItem(int itemId) {
        ChecklistItem item = findItemById(itemId);
        checklistItemRepository.delete(item);
    }

    @Override
    @Transactional(readOnly = true)
    public ChecklistResponse getChecklistProgress(int checklistId) {
        /*
         * Returns the checklist WITH its items so ChecklistResponse.from() can
         * calculate completedCount, totalCount, and progressPercent.
         *
         * This method is called when the frontend needs to refresh the progress bar
         * after a user ticks or un-ticks an item, without reloading the whole card.
         */
        Checklist checklist = findChecklistById(checklistId);
        return ChecklistResponse.from(checklist);
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    /**
     * Fetches a Checklist from the database or throws a 404 error.
     *
     * WHY EXTRACT THIS INTO A HELPER?
     *   Multiple methods need to do the same "find or throw 404" logic.
     *   Extracting it avoids copy-pasting the same orElseThrow block everywhere.
     *   If we ever want to change the error message, we change it in one place.
     */
    private Checklist findChecklistById(int checklistId) {
        return checklistRepository.findById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Checklist not found with id: " + checklistId
                ));
    }

    /**
     * Fetches a ChecklistItem from the database or throws a 404 error.
     */
    private ChecklistItem findItemById(int itemId) {
        return checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Checklist item not found with id: " + itemId
                ));
    }
}