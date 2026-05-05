package com.app.taskmanagement.card.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.app.taskmanagement.card.entity.Card;

/**
 * What the API returns to the client for a card.
 *
 * We always return DTOs instead of raw entities because: - We control exactly
 * what fields are exposed - We can add computed fields (like attachmentCount)
 * without changing the entity - We can evolve the DB schema without breaking
 * the API contract
 */
@Data
@Builder
public class CardResponse {

	private Integer cardId;
	private Integer listId;
	private Integer boardId;
	private Integer workspaceId;
	private String title;
	private String description;
	private String priority;
	private String status;
	private Integer assigneeUserId;
	private Integer createdByUserId;
	private LocalDate dueDate;
	private LocalDate startDate;
	private String coverColor;
	private Integer position;
	private boolean archived;

	/**
	 * Number of attachments on this card. Always included — lightweight to show a
	 * count badge in the UI.
	 */
	private int attachmentCount;

	/**
	 * Full attachment list — only included when opening a single card detail view.
	 * Omitted in list views to keep responses lightweight.
	 */
	private List<CardAttachmentResponse> attachments;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	/**
	 * Convert Card entity → CardResponse DTO.
	 *
	 * @param card           the entity from DB
	 * @param includeDetails true = include full attachment list (card detail view)
	 *                       false = lightweight (board/list view)
	 */
	public static CardResponse from(Card card, boolean includeDetails) {
		return CardResponse.builder().cardId(card.getCardId()).listId(card.getListId()).boardId(card.getBoardId())
				.workspaceId(card.getWorkspaceId()).title(card.getTitle()).description(card.getDescription())
				.priority(card.getPriority().name()).status(card.getStatus().name())
				.assigneeUserId(card.getAssigneeUserId()).createdByUserId(card.getCreatedByUserId())
				.dueDate(card.getDueDate()).startDate(card.getStartDate()).coverColor(card.getCoverColor())
				.position(card.getPosition()).archived(card.isArchived()).attachmentCount(card.getAttachments().size())
				.attachments(includeDetails ? card.getAttachments().stream().map(CardAttachmentResponse::from).toList()
						: null)     
				.createdAt(card.getCreatedAt()).updatedAt(card.getUpdatedAt()).build();
	}
}