package com.app.taskmanagement.label.service;

import com.app.taskmanagement.label.dto.*;
import com.app.taskmanagement.label.entity.*;
import com.app.taskmanagement.label.exception.*;
import com.app.taskmanagement.label.repository.*;
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
public class LabelServiceImplTest {

    @Mock private LabelRepository labelRepository;
    @Mock private CardLabelRepository cardLabelRepository;
    @Mock private ChecklistRepository checklistRepository;
    @Mock private ChecklistItemRepository checklistItemRepository;

    @InjectMocks
    private LabelServiceImpl labelService;

    private Label testLabel;
    private Checklist testChecklist;
    private ChecklistItem testItem;

    @BeforeEach
    void setUp() {
        testLabel = Label.builder().labelId(1).boardId(5).name("Bug").color("#FF5733").build();
        testItem = ChecklistItem.builder().itemId(1).text("Write tests").completed(false).position(1).build();
        testChecklist = Checklist.builder().checklistId(1).cardId(10).title("Backend tasks")
                .position(1).items(new ArrayList<>(List.of(testItem))).build();
        testItem.setChecklist(testChecklist);
    }

    @AfterEach
    void tearDown() { testLabel = null; testChecklist = null; testItem = null; }

    @Test
    @DisplayName("createLabel - success with unique name")
    void createLabel_Success() {
        LabelRequest req = new LabelRequest(); req.setName("Bug"); req.setColor("#FF5733");
        when(labelRepository.existsByBoardIdAndNameIgnoreCase(5, "Bug")).thenReturn(false);
        when(labelRepository.save(any(Label.class))).thenReturn(testLabel);
        LabelResponse res = labelService.createLabel(5, req);
        assertNotNull(res); assertEquals("Bug", res.getName());
    }

    @Test
    @DisplayName("createLabel - throws for duplicate name")
    void createLabel_Duplicate() {
        LabelRequest req = new LabelRequest(); req.setName("Bug"); req.setColor("#FF5733");
        when(labelRepository.existsByBoardIdAndNameIgnoreCase(5, "Bug")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> labelService.createLabel(5, req));
    }

    @Test
    @DisplayName("getLabelsByBoard - returns labels")
    void getLabelsByBoard_Success() {
        when(labelRepository.findAllByBoardIdOrderByCreatedAtAsc(5)).thenReturn(List.of(testLabel));
        assertEquals(1, labelService.getLabelsByBoard(5).size());
    }

    @Test
    @DisplayName("updateLabel - success")
    void updateLabel_Success() {
        LabelRequest req = new LabelRequest(); req.setName("Defect"); req.setColor("#00FF00");
        when(labelRepository.findByLabelIdAndBoardId(1, 5)).thenReturn(Optional.of(testLabel));
        when(labelRepository.existsByBoardIdAndNameIgnoreCase(5, "Defect")).thenReturn(false);
        when(labelRepository.save(any(Label.class))).thenReturn(testLabel);
        labelService.updateLabel(5, 1, req);
        assertEquals("Defect", testLabel.getName());
    }

    @Test
    @DisplayName("updateLabel - not found")
    void updateLabel_NotFound() {
        LabelRequest req = new LabelRequest(); req.setName("X"); req.setColor("#000000");
        when(labelRepository.findByLabelIdAndBoardId(999, 5)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> labelService.updateLabel(5, 999, req));
    }

    @Test
    @DisplayName("deleteLabel - removes associations then label")
    void deleteLabel_Success() {
        when(labelRepository.findByLabelIdAndBoardId(1, 5)).thenReturn(Optional.of(testLabel));
        labelService.deleteLabel(5, 1);
        verify(cardLabelRepository).deleteAllByCardId(1);
        verify(labelRepository).delete(testLabel);
    }

    @Test
    @DisplayName("addLabelToCard - success")
    void addLabelToCard_Success() {
        when(labelRepository.findById(1)).thenReturn(Optional.of(testLabel));
        when(cardLabelRepository.existsByCardIdAndLabelLabelId(10, 1)).thenReturn(false);
        labelService.addLabelToCard(10, 1);
        verify(cardLabelRepository).save(any(CardLabel.class));
    }

    @Test
    @DisplayName("addLabelToCard - duplicate throws")
    void addLabelToCard_Duplicate() {
        when(labelRepository.findById(1)).thenReturn(Optional.of(testLabel));
        when(cardLabelRepository.existsByCardIdAndLabelLabelId(10, 1)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> labelService.addLabelToCard(10, 1));
    }

    @Test
    @DisplayName("removeLabelFromCard - success")
    void removeLabelFromCard_Success() {
        when(cardLabelRepository.existsByCardIdAndLabelLabelId(10, 1)).thenReturn(true);
        labelService.removeLabelFromCard(10, 1);
        verify(cardLabelRepository).deleteByCardIdAndLabelId(10, 1);
    }

    @Test
    @DisplayName("removeLabelFromCard - not on card throws")
    void removeLabelFromCard_NotOnCard() {
        when(cardLabelRepository.existsByCardIdAndLabelLabelId(10, 1)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> labelService.removeLabelFromCard(10, 1));
    }

    @Test
    @DisplayName("createChecklist - auto-assigns position")
    void createChecklist_Success() {
        CreateChecklistRequest req = new CreateChecklistRequest(); req.setTitle("Backend tasks");
        when(checklistRepository.findMaxPositionByCardId(10)).thenReturn(0);
        when(checklistRepository.save(any(Checklist.class))).thenReturn(testChecklist);
        ChecklistResponse res = labelService.createChecklist(10, req);
        assertEquals("Backend tasks", res.getTitle());
    }

    @Test
    @DisplayName("toggleItem - flips completed state")
    void toggleItem_Success() {
        assertFalse(testItem.isCompleted());
        when(checklistItemRepository.findById(1)).thenReturn(Optional.of(testItem));
        when(checklistItemRepository.save(any(ChecklistItem.class))).thenReturn(testItem);
        labelService.toggleItem(1);
        assertTrue(testItem.isCompleted());
    }

    @Test
    @DisplayName("toggleItem - un-toggles completed item")
    void toggleItem_Untoggle() {
        testItem.setCompleted(true);
        when(checklistItemRepository.findById(1)).thenReturn(Optional.of(testItem));
        when(checklistItemRepository.save(any(ChecklistItem.class))).thenReturn(testItem);
        labelService.toggleItem(1);
        assertFalse(testItem.isCompleted());
    }

    @Test
    @DisplayName("deleteItem - deletes item")
    void deleteItem_Success() {
        when(checklistItemRepository.findById(1)).thenReturn(Optional.of(testItem));
        labelService.deleteItem(1);
        verify(checklistItemRepository).delete(testItem);
    }

    @Test
    @DisplayName("getChecklistProgress - computes 100% when all done")
    void getChecklistProgress_AllDone() {
        testItem.setCompleted(true);
        when(checklistRepository.findById(1)).thenReturn(Optional.of(testChecklist));
        ChecklistResponse res = labelService.getChecklistProgress(1);
        assertEquals(100, res.getProgressPercent());
        assertEquals(1, res.getCompletedCount());
    }
}
