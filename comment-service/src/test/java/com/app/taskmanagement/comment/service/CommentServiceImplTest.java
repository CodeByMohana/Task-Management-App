package com.app.taskmanagement.comment.service;

import com.app.taskmanagement.comment.dto.*;
import com.app.taskmanagement.comment.entity.Attachment;
import com.app.taskmanagement.comment.entity.Comment;
import com.app.taskmanagement.comment.exception.*;
import com.app.taskmanagement.comment.repository.AttachmentRepository;
import com.app.taskmanagement.comment.repository.CommentRepository;
import com.app.taskmanagement.comment.messaging.NotificationPublisher;
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
public class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment topLevelComment;
    private Comment replyComment;

    @BeforeEach
    void setUp() {
        topLevelComment = Comment.builder()
                .commentId(1)
                .cardId(10)
                .authorUserId(100)
                .content("This is a top-level comment")
                .parentCommentId(null)
                .deleted(false)
                .replies(new ArrayList<>())
                .build();

        replyComment = Comment.builder()
                .commentId(2)
                .cardId(10)
                .authorUserId(200)
                .content("This is a reply")
                .parentCommentId(1)
                .deleted(false)
                .replies(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        topLevelComment = null;
        replyComment = null;
    }

    // ─── Add Comment Tests ───────────────────────────────────────────

    @Test
    @DisplayName("addComment - should create top-level comment successfully")
    void addComment_TopLevel_Success() {
        AddCommentRequest request = new AddCommentRequest();
        request.setContent("New comment");

        when(commentRepository.save(any(Comment.class))).thenReturn(topLevelComment);

        CommentResponse response = commentService.addComment(10, request, 100);

        assertNotNull(response);
        assertEquals(10, response.getCardId());
        assertNull(response.getParentCommentId());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("addComment - should create reply and validate parent exists")
    void addComment_Reply_Success() {
        AddCommentRequest request = new AddCommentRequest();
        request.setContent("Reply text");
        request.setParentCommentId(1);

        when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(replyComment);

        CommentResponse response = commentService.addComment(10, request, 200);

        assertNotNull(response);
        assertEquals(1, response.getParentCommentId());
    }

    @Test
    @DisplayName("addComment - should throw ResourceNotFoundException for non-existent parent")
    void addComment_Reply_ParentNotFound() {
        AddCommentRequest request = new AddCommentRequest();
        request.setContent("Reply text");
        request.setParentCommentId(999);

        when(commentRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.addComment(10, request, 200));
    }

    @Test
    @DisplayName("addComment - should throw BadRequestException when parent belongs to different card")
    void addComment_Reply_CrossCard() {
        AddCommentRequest request = new AddCommentRequest();
        request.setContent("Reply text");
        request.setParentCommentId(1);

        when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));

        assertThrows(BadRequestException.class,
                () -> commentService.addComment(99, request, 200)); // cardId 99 != parent's cardId 10
    }

    @Test
    @DisplayName("addComment - should throw BadRequestException when replying to a reply (nesting)")
    void addComment_Reply_NestedReply() {
        AddCommentRequest request = new AddCommentRequest();
        request.setContent("Reply to reply");
        request.setParentCommentId(2);

        when(commentRepository.findById(2)).thenReturn(Optional.of(replyComment));

        assertThrows(BadRequestException.class,
                () -> commentService.addComment(10, request, 300));
    }

    // ─── Get / Read Tests ────────────────────────────────────────────

    @Test
    @DisplayName("getCommentsByCard - should return top-level comments")
    void getCommentsByCard_Success() {
        when(commentRepository.findAllByCardIdAndParentCommentIdIsNullOrderByCreatedAtAsc(10))
                .thenReturn(List.of(topLevelComment));

        List<CommentResponse> result = commentService.getCommentsByCard(10, 100);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isOwner());
    }

    @Test
    @DisplayName("getCommentById - should return comment with owner flag")
    void getCommentById_Success() {
        when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));

        CommentResponse response = commentService.getCommentById(1, 100);

        assertNotNull(response);
        assertTrue(response.isOwner());
    }

    @Test
    @DisplayName("getCommentById - should throw ResourceNotFoundException for non-existent")
    void getCommentById_NotFound() {
        when(commentRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.getCommentById(999, 100));
    }

    @Test
    @DisplayName("getCommentCount - should return count from repository")
    void getCommentCount_Success() {
        when(commentRepository.countActiveByCardId(10)).thenReturn(5);

        int count = commentService.getCommentCount(10);

        assertEquals(5, count);
    }

    // ─── Update Comment Tests ────────────────────────────────────────

    @Test
    @DisplayName("updateComment - should update content by owner")
    void updateComment_Success() {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated comment");

        when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(topLevelComment);

        CommentResponse response = commentService.updateComment(1, request, 100);

        assertNotNull(response);
        assertEquals("Updated comment", topLevelComment.getContent());
    }

    @Test
    @DisplayName("updateComment - should throw ForbiddenException when not the author")
    void updateComment_NotOwner() {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated comment");

        when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));

        assertThrows(ForbiddenException.class,
                () -> commentService.updateComment(1, request, 200));
    }

    @Test
    @DisplayName("updateComment - should throw BadRequestException for deleted comment")
    void updateComment_DeletedComment() {
        topLevelComment.setDeleted(true);
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated comment");

        when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));

        assertThrows(BadRequestException.class,
                () -> commentService.updateComment(1, request, 100));
    }

    // ─── Delete Comment Tests ────────────────────────────────────────

    @Test
    @DisplayName("deleteComment - should soft-delete and blank content")
    void deleteComment_Success() {
        when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(topLevelComment);

        commentService.deleteComment(1, 100);

        assertTrue(topLevelComment.isDeleted());
        assertEquals("", topLevelComment.getContent());
    }

    @Test
    @DisplayName("deleteComment - should throw BadRequestException if already deleted")
    void deleteComment_AlreadyDeleted() {
        topLevelComment.setDeleted(true);
        when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));

        assertThrows(BadRequestException.class,
                () -> commentService.deleteComment(1, 100));
    }

    // ─── Attachment Tests ────────────────────────────────────────────

    @Test
    @DisplayName("addAttachment - should save attachment metadata")
    void addAttachment_Success() {
        AddAttachmentRequest request = new AddAttachmentRequest();
        request.setFileName("report.pdf");
        request.setFileUrl("https://cdn.app.com/report.pdf");
        request.setFileType("application/pdf");
        request.setFileSizeKb(500L);

        Attachment saved = Attachment.builder()
                .attachmentId(1).cardId(10).uploaderUserId(100)
                .fileName("report.pdf").fileUrl("https://cdn.app.com/report.pdf")
                .fileType("application/pdf").fileSizeKb(500L).build();

        when(attachmentRepository.save(any(Attachment.class))).thenReturn(saved);

        AttachmentResponse response = commentService.addAttachment(10, request, 100);

        assertNotNull(response);
        assertEquals("report.pdf", response.getFileName());
        assertTrue(response.isOwner());
    }

    @Test
    @DisplayName("deleteAttachment - should throw ForbiddenException for non-uploader")
    void deleteAttachment_ForbiddenNonUploader() {
        Attachment attachment = Attachment.builder()
                .attachmentId(5).cardId(10).uploaderUserId(100).build();

        when(attachmentRepository.findByAttachmentIdAndCardId(5, 10))
                .thenReturn(Optional.of(attachment));

        assertThrows(ForbiddenException.class,
                () -> commentService.deleteAttachment(10, 5, 200));
    }
}
