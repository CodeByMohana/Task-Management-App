package com.app.taskmanagement.card.service;

import com.app.taskmanagement.card.dto.*;
import com.app.taskmanagement.card.entity.Card;
import com.app.taskmanagement.card.entity.CardAttachment;
import com.app.taskmanagement.card.exception.*;
import com.app.taskmanagement.card.repository.CardAttachmentRepository;
import com.app.taskmanagement.card.repository.CardRepository;
import com.app.taskmanagement.card.messaging.NotificationPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardAttachmentRepository attachmentRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private CardServiceImpl cardService;

    private Card testCard;

    @BeforeEach
    void setUp() {
        testCard = Card.builder()
                .cardId(1)
                .listId(10)
                .boardId(5)
                .workspaceId(1)
                .title("Fix login bug")
                .description("Login page has a CSS issue")
                .priority(Card.Priority.HIGH)
                .status(Card.Status.TO_DO)
                .createdByUserId(100)
                .position(1)
                .archived(false)
                .attachments(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        testCard = null;
    }

    // ─── Card CRUD ────────────────────────────────────────────────────

    @Test
    @DisplayName("createCard - should create card with auto-assigned position")
    void createCard_Success() {
        CreateCardRequest request = new CreateCardRequest();
        request.setListId(10);
        request.setBoardId(5);
        request.setWorkspaceId(1);
        request.setTitle("Fix login bug");

        when(cardRepository.findMaxPositionByListId(10)).thenReturn(0);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse response = cardService.createCard(request, 100);

        assertNotNull(response);
        assertEquals("Fix login bug", response.getTitle());
        assertEquals("TO_DO", response.getStatus());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("createCard - should notify assignee when card is assigned to someone else")
    void createCard_NotifiesAssignee() {
        CreateCardRequest request = new CreateCardRequest();
        request.setListId(10);
        request.setBoardId(5);
        request.setWorkspaceId(1);
        request.setTitle("Fix login bug");
        request.setAssigneeUserId(200);

        testCard.setAssigneeUserId(200);

        when(cardRepository.findMaxPositionByListId(10)).thenReturn(0);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        cardService.createCard(request, 100);

        verify(notificationPublisher).notifyCardAssigned(eq(200), eq("Fix login bug"),
                eq(100), anyLong(), anyString(), any(), any());
    }

    @Test
    @DisplayName("getCard - should return card with details")
    void getCard_Success() {
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));

        CardResponse response = cardService.getCard(1, 100);

        assertNotNull(response);
        assertEquals(1, response.getCardId());
    }

    @Test
    @DisplayName("getCard - should throw ResourceNotFoundException for non-existent card")
    void getCard_NotFound() {
        when(cardRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cardService.getCard(999, 100));
    }

    @Test
    @DisplayName("getCardsByList - should return cards for a list")
    void getCardsByList_Success() {
        when(cardRepository.findAllByListIdAndArchivedFalseOrderByPositionAsc(10))
                .thenReturn(List.of(testCard));

        List<CardResponse> result = cardService.getCardsByList(10);

        assertEquals(1, result.size());
        assertEquals("Fix login bug", result.get(0).getTitle());
    }

    @Test
    @DisplayName("updateCard - should update title only via PATCH-style")
    void updateCard_PatchTitle() {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setTitle("Updated title");

        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse response = cardService.updateCard(1, request, 100);

        assertNotNull(response);
        assertEquals("Updated title", testCard.getTitle());
    }

    // ─── Card Movement ───────────────────────────────────────────────

    @Test
    @DisplayName("moveCard - same list reorder should shift positions")
    void moveCard_SameListReorder() {
        MoveCardRequest request = new MoveCardRequest();
        request.setTargetListId(10); // same list
        request.setNewPosition(3);

        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        cardService.moveCard(1, request, 100);

        assertEquals(3, testCard.getPosition());
        verify(cardRepository).shiftPositionsUp(10, 3, 1);
    }

    @Test
    @DisplayName("moveCard - cross-list move should update listId and status")
    void moveCard_CrossList() {
        MoveCardRequest request = new MoveCardRequest();
        request.setTargetListId(20); // different list
        request.setNewPosition(1);

        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        cardService.moveCard(1, request, 100);

        assertEquals(20, testCard.getListId());
        assertEquals(1, testCard.getPosition());
        assertEquals(Card.Status.IN_PROGRESS, testCard.getStatus());
        verify(cardRepository).shiftPositionsDown(10, 1, 1);
        verify(cardRepository).shiftPositionsUp(20, 1, 1);
    }

    // ─── Archive / Delete ────────────────────────────────────────────

    @Test
    @DisplayName("archiveCard - should archive a non-archived card")
    void archiveCard_Success() {
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        cardService.archiveCard(1, 100);

        assertTrue(testCard.isArchived());
    }

    @Test
    @DisplayName("archiveCard - should throw BadRequestException if already archived")
    void archiveCard_AlreadyArchived() {
        testCard.setArchived(true);
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));

        assertThrows(BadRequestException.class,
                () -> cardService.archiveCard(1, 100));
    }

    @Test
    @DisplayName("unarchiveCard - should unarchive and append to end of list")
    void unarchiveCard_Success() {
        testCard.setArchived(true);
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(cardRepository.findMaxPositionByListId(10)).thenReturn(5);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        cardService.unarchiveCard(1, 100);

        assertFalse(testCard.isArchived());
        assertEquals(6, testCard.getPosition());
    }

    @Test
    @DisplayName("deleteCard - should delete archived card")
    void deleteCard_Success() {
        testCard.setArchived(true);
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));

        cardService.deleteCard(1, 100);

        verify(cardRepository).delete(testCard);
    }

    @Test
    @DisplayName("deleteCard - should throw BadRequestException for non-archived card")
    void deleteCard_NotArchived() {
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));

        assertThrows(BadRequestException.class,
                () -> cardService.deleteCard(1, 100));
        verify(cardRepository, never()).delete(any());
    }

    // ─── Attachment Tests ────────────────────────────────────────────

    @Test
    @DisplayName("addAttachment - should add attachment to non-archived card")
    void addAttachment_Success() {
        AddAttachmentRequest request = new AddAttachmentRequest();
        request.setFileName("report.pdf");
        request.setFileType("application/pdf");
        request.setFileSize(1024L);
        request.setFileUrl("https://s3.amazonaws.com/report.pdf");

        CardAttachment saved = CardAttachment.builder()
                .attachmentId(1).card(testCard).fileName("report.pdf")
                .fileType("application/pdf").fileSize(1024L)
                .fileUrl("https://s3.amazonaws.com/report.pdf")
                .uploadedByUserId(100).build();

        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(attachmentRepository.save(any(CardAttachment.class))).thenReturn(saved);

        CardAttachmentResponse response = cardService.addAttachment(1, request, 100);

        assertNotNull(response);
        assertEquals("report.pdf", response.getFileName());
    }

    @Test
    @DisplayName("addAttachment - should throw BadRequestException for archived card")
    void addAttachment_ArchivedCard() {
        testCard.setArchived(true);
        AddAttachmentRequest request = new AddAttachmentRequest();
        request.setFileName("report.pdf");

        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));

        assertThrows(BadRequestException.class,
                () -> cardService.addAttachment(1, request, 100));
    }

    @Test
    @DisplayName("deleteAttachment - should throw ForbiddenException when not uploader")
    void deleteAttachment_ForbiddenNonUploader() {
        CardAttachment attachment = CardAttachment.builder()
                .attachmentId(5).card(testCard).uploadedByUserId(100).build();

        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(attachmentRepository.findByAttachmentIdAndCardCardId(5, 1))
                .thenReturn(Optional.of(attachment));

        assertThrows(ForbiddenException.class,
                () -> cardService.deleteAttachment(1, 5, 200));
    }
}
