package com.app.taskmanagement.notification.service;

import com.app.taskmanagement.notification.dto.NotificationEvent;
import com.app.taskmanagement.notification.dto.NotificationPage;
import com.app.taskmanagement.notification.entity.Notification;
import com.app.taskmanagement.notification.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailService emailService;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private NotificationEvent testEvent;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L).eventType("CARD_ASSIGNED").recipientEmail("user@example.com")
                .recipientName("John").subject("Card assigned").message("You were assigned")
                .status(Notification.NotificationStatus.PENDING).isRead(false)
                .createdAt(LocalDateTime.now()).build();

        testEvent = NotificationEvent.builder()
                .eventType("CARD_ASSIGNED").recipientEmail("user@example.com")
                .recipientName("John").subject("Card assigned").message("You were assigned")
                .triggeredBy("Admin").build();
    }

    @AfterEach
    void tearDown() { testNotification = null; testEvent = null; }

    @Test
    @DisplayName("processNotification - saves notification and sends email")
    void processNotification_Success() {
        when(emailTemplateService.buildHtmlEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("<html>email</html>");
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.processNotification(testEvent);

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(emailService).sendHtmlEmail(eq("user@example.com"), eq("Card assigned"), anyString());
    }

    @Test
    @DisplayName("processNotification - OTP email only sends email, no DB save")
    void processNotification_OtpEmail() {
        testEvent.setEventType("OTP_EMAIL");
        when(emailTemplateService.buildHtmlEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("<html>otp</html>");

        notificationService.processNotification(testEvent);

        verify(emailService).sendHtmlEmail(eq("user@example.com"), eq("Card assigned"), anyString());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("processNotification - handles email failure gracefully")
    void processNotification_EmailFailure() {
        when(emailTemplateService.buildHtmlEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("<html>email</html>");
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        doThrow(new RuntimeException("SMTP error")).when(emailService)
                .sendHtmlEmail(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> notificationService.processNotification(testEvent));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("getUserNotifications - returns paginated notifications")
    void getUserNotifications_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(testNotification), pageable, 1);
        when(notificationRepository.findByRecipientEmailOrderByCreatedAtDesc("user@example.com", pageable))
                .thenReturn(page);

        NotificationPage result = notificationService.getUserNotifications("user@example.com", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getUserNotifications - returns empty page when no notifications")
    void getUserNotifications_Empty() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(), pageable, 0);
        when(notificationRepository.findByRecipientEmailOrderByCreatedAtDesc("new@example.com", pageable))
                .thenReturn(page);

        NotificationPage result = notificationService.getUserNotifications("new@example.com", pageable);

        assertEquals(0, result.getContent().size());
    }

    @Test
    @DisplayName("getUnreadCount - returns count from repository")
    void getUnreadCount_Success() {
        when(notificationRepository.countByRecipientEmailAndIsRead("user@example.com", false))
                .thenReturn(5L);

        long count = notificationService.getUnreadCount("user@example.com");

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("getUnreadCount - returns zero when all read")
    void getUnreadCount_Zero() {
        when(notificationRepository.countByRecipientEmailAndIsRead("user@example.com", false))
                .thenReturn(0L);

        assertEquals(0L, notificationService.getUnreadCount("user@example.com"));
    }

    @Test
    @DisplayName("markAsRead - marks notification as read by matching email")
    void markAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        notificationService.markAsRead(1L, "user@example.com");

        assertTrue(testNotification.getIsRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("markAsRead - does not mark if email doesn't match")
    void markAsRead_EmailMismatch() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        notificationService.markAsRead(1L, "other@example.com");

        assertFalse(testNotification.getIsRead());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsRead - does nothing for non-existent notification")
    void markAsRead_NotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> notificationService.markAsRead(999L, "user@example.com"));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAllAsRead - delegates to repository")
    void markAllAsRead_Success() {
        notificationService.markAllAsRead("user@example.com");

        verify(notificationRepository).markAllAsReadByEmail("user@example.com");
    }
}
