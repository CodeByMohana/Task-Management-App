package com.app.taskmanagement.boardservice.service;

import com.app.taskmanagement.boardservice.dto.*;
import com.app.taskmanagement.boardservice.entity.Board;
import com.app.taskmanagement.boardservice.entity.BoardList;
import com.app.taskmanagement.boardservice.entity.BoardMember;
import com.app.taskmanagement.boardservice.exception.*;
import com.app.taskmanagement.boardservice.repository.BoardListRepository;
import com.app.taskmanagement.boardservice.repository.BoardMemberRepository;
import com.app.taskmanagement.boardservice.repository.BoardRepository;
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
public class BoardServiceImplTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private BoardListRepository boardListRepository;

    @InjectMocks
    private BoardServiceImpl boardService;

    private Board testBoard;
    private BoardMember adminMember;
    private BoardMember regularMember;

    @BeforeEach
    void setUp() {
        testBoard = Board.builder()
                .boardId(1)
                .workspaceId(10)
                .name("Sprint 1")
                .description("Sprint board")
                .visibility(Board.Visibility.WORKSPACE)
                .closed(false)
                .createdByUserId(100)
                .members(new ArrayList<>())
                .lists(new ArrayList<>())
                .build();

        adminMember = BoardMember.builder()
                .id(1)
                .board(testBoard)
                .userId(100)
                .role(BoardMember.Role.ADMIN)
                .build();

        regularMember = BoardMember.builder()
                .id(2)
                .board(testBoard)
                .userId(200)
                .role(BoardMember.Role.MEMBER)
                .build();

        testBoard.getMembers().add(adminMember);
    }

    @AfterEach
    void tearDown() {
        testBoard = null;
        adminMember = null;
        regularMember = null;
    }

    // ─── Board CRUD ───────────────────────────────────────────────────

    @Test
    @DisplayName("createBoard - should create board and add creator as ADMIN")
    void createBoard_Success() {
        CreateBoardRequest request = new CreateBoardRequest();
        request.setWorkspaceId(10);
        request.setName("Sprint 1");
        request.setDescription("Sprint board");

        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(boardMemberRepository.save(any(BoardMember.class))).thenReturn(adminMember);

        BoardResponse response = boardService.createBoard(request, 100);

        assertNotNull(response);
        assertEquals("Sprint 1", response.getName());
        verify(boardRepository).save(any(Board.class));
        verify(boardMemberRepository).save(any(BoardMember.class));
    }

    @Test
    @DisplayName("getBoard - should return board for board member")
    void getBoard_AsBoardMember() {
        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.existsByBoardBoardIdAndUserId(1, 100)).thenReturn(true);

        BoardResponse response = boardService.getBoard(1, 100, List.of());

        assertNotNull(response);
        assertEquals(1, response.getBoardId());
    }

    @Test
    @DisplayName("getBoard - should throw ResourceNotFoundException for non-existent board")
    void getBoard_NotFound() {
        when(boardRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> boardService.getBoard(999, 100, List.of()));
    }

    @Test
    @DisplayName("getBoard - should throw ForbiddenException for private board and non-member")
    void getBoard_ForbiddenForPrivateBoard() {
        testBoard.setVisibility(Board.Visibility.PRIVATE);
        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.existsByBoardBoardIdAndUserId(1, 300)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> boardService.getBoard(1, 300, List.of(100, 200)));
    }

    @Test
    @DisplayName("updateBoard - should update board name when admin")
    void updateBoard_Success() {
        UpdateBoardRequest request = new UpdateBoardRequest();
        request.setName("Updated Sprint");

        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardBoardIdAndUserId(1, 100))
                .thenReturn(Optional.of(adminMember));
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

        BoardResponse response = boardService.updateBoard(1, request, 100);

        assertNotNull(response);
        verify(boardRepository).save(any(Board.class));
    }

    @Test
    @DisplayName("updateBoard - should throw ForbiddenException for non-admin")
    void updateBoard_ForbiddenForNonAdmin() {
        UpdateBoardRequest request = new UpdateBoardRequest();
        request.setName("Updated Sprint");

        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardBoardIdAndUserId(1, 200))
                .thenReturn(Optional.of(regularMember));

        assertThrows(ForbiddenException.class,
                () -> boardService.updateBoard(1, request, 200));
    }

    @Test
    @DisplayName("closeBoard - should close an open board")
    void closeBoard_Success() {
        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardBoardIdAndUserId(1, 100))
                .thenReturn(Optional.of(adminMember));
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

        BoardResponse response = boardService.closeBoard(1, 100);

        assertNotNull(response);
        verify(boardRepository).save(any(Board.class));
    }

    @Test
    @DisplayName("closeBoard - should throw BadRequestException if already closed")
    void closeBoard_AlreadyClosed() {
        testBoard.setClosed(true);
        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardBoardIdAndUserId(1, 100))
                .thenReturn(Optional.of(adminMember));

        assertThrows(BadRequestException.class,
                () -> boardService.closeBoard(1, 100));
    }

    @Test
    @DisplayName("deleteBoard - should delete board by creator")
    void deleteBoard_ByCreator() {
        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));

        boardService.deleteBoard(1, 100);

        verify(boardRepository).delete(testBoard);
    }

    @Test
    @DisplayName("deleteBoard - should throw ForbiddenException for non-creator")
    void deleteBoard_ForbiddenForNonCreator() {
        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));

        assertThrows(ForbiddenException.class,
                () -> boardService.deleteBoard(1, 200));
        verify(boardRepository, never()).delete(any());
    }

    // ─── Board Member Tests ──────────────────────────────────────────

    @Test
    @DisplayName("addBoardMember - should add member when admin and no duplicate")
    void addBoardMember_Success() {
        AddBoardMemberRequest request = new AddBoardMemberRequest();
        request.setUserId(300);
        request.setRole(BoardMember.Role.MEMBER);

        BoardMember newMember = BoardMember.builder()
                .id(3).board(testBoard).userId(300).role(BoardMember.Role.MEMBER).build();

        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardBoardIdAndUserId(1, 100))
                .thenReturn(Optional.of(adminMember));
        when(boardMemberRepository.existsByBoardBoardIdAndUserId(1, 300)).thenReturn(false);
        when(boardRepository.getReferenceById(1)).thenReturn(testBoard);
        when(boardMemberRepository.save(any(BoardMember.class))).thenReturn(newMember);

        BoardMemberResponse response = boardService.addBoardMember(1, request, 100);

        assertNotNull(response);
        assertEquals(300, response.getUserId());
    }

    @Test
    @DisplayName("addBoardMember - should throw DuplicateResourceException for existing member")
    void addBoardMember_Duplicate() {
        AddBoardMemberRequest request = new AddBoardMemberRequest();
        request.setUserId(200);
        request.setRole(BoardMember.Role.MEMBER);

        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardBoardIdAndUserId(1, 100))
                .thenReturn(Optional.of(adminMember));
        when(boardMemberRepository.existsByBoardBoardIdAndUserId(1, 200)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> boardService.addBoardMember(1, request, 100));
    }

    // ─── List (Column) Tests ─────────────────────────────────────────

    @Test
    @DisplayName("createList - should create list on open board")
    void createList_Success() {
        CreateListRequest request = new CreateListRequest();
        request.setName("To Do");

        BoardList savedList = BoardList.builder()
                .listId(1).board(testBoard).name("To Do").position(1).build();

        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));
        when(boardMemberRepository.findByBoardBoardIdAndUserId(1, 100))
                .thenReturn(Optional.of(adminMember));
        when(boardListRepository.findMaxPositionByBoardId(1)).thenReturn(0);
        when(boardListRepository.save(any(BoardList.class))).thenReturn(savedList);

        BoardListResponse response = boardService.createList(1, request, 100);

        assertNotNull(response);
        assertEquals("To Do", response.getName());
    }

    @Test
    @DisplayName("createList - should throw BadRequestException on closed board")
    void createList_OnClosedBoard() {
        testBoard.setClosed(true);
        CreateListRequest request = new CreateListRequest();
        request.setName("To Do");

        when(boardRepository.findById(1)).thenReturn(Optional.of(testBoard));

        assertThrows(BadRequestException.class,
                () -> boardService.createList(1, request, 100));
    }
}
