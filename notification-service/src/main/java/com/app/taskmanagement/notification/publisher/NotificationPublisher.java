package com.app.taskmanagement.notification.publisher;

import com.app.taskmanagement.notification.config.RabbitMQConfig;
import com.app.taskmanagement.notification.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Publisher utility for sending notification events to RabbitMQ.
 * Other services (card-service, board-service, etc.) can use this to trigger notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishNotification(NotificationEvent event) {
        event.setTimestamp(LocalDateTime.now());
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.NOTIFICATION_EXCHANGE,
            RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
            event
        );
        
        log.info("Published notification event: {} to user: {}", event.getEventType(), event.getRecipientEmail());
    }
    
    // Helper methods for common notification types
    
    public void notifyCardAssigned(String recipientEmail, String recipientName, 
                                   String cardTitle, String assignedBy, Long cardId, String workspaceId) {
        NotificationEvent event = NotificationEvent.builder()
                .eventType("CARD_ASSIGNED")
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .subject("You've been assigned to a card")
                .message(String.format("Hi %s,\n\n%s has assigned you to the card '%s'.\n\nCheck it out now!",
                        recipientName, assignedBy, cardTitle))
                .entityId(cardId)
                .entityType("CARD")
                .workspaceId(workspaceId)
                .triggeredBy(assignedBy)
                .build();
        
        publishNotification(event);
    }
    
    public void notifyCardDueDateApproaching(String recipientEmail, String recipientName,
                                             String cardTitle, Long cardId, String workspaceId) {
        NotificationEvent event = NotificationEvent.builder()
                .eventType("CARD_DUE_DATE_APPROACHING")
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .subject("Card due date approaching")
                .message(String.format("Hi %s,\n\nThe card '%s' is due soon. Please complete it on time.",
                        recipientName, cardTitle))
                .entityId(cardId)
                .entityType("CARD")
                .workspaceId(workspaceId)
                .build();
        
        publishNotification(event);
    }
    
    public void notifyCommentAdded(String recipientEmail, String recipientName,
                                   String cardTitle, String commenter, String commentText,
                                   Long commentId, String workspaceId) {
        NotificationEvent event = NotificationEvent.builder()
                .eventType("COMMENT_ADDED")
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .subject("New comment on your card")
                .message(String.format("Hi %s,\n\n%s commented on '%s':\n\n\"%s\"",
                        recipientName, commenter, cardTitle, commentText))
                .entityId(commentId)
                .entityType("COMMENT")
                .workspaceId(workspaceId)
                .triggeredBy(commenter)
                .build();
        
        publishNotification(event);
    }
    
    public void notifyWorkspaceMemberAdded(String recipientEmail, String recipientName,
                                          String workspaceName, String addedBy, String workspaceId) {
        NotificationEvent event = NotificationEvent.builder()
                .eventType("WORKSPACE_MEMBER_ADDED")
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .subject("You've been added to a workspace")
                .message(String.format("Hi %s,\n\n%s has added you to the workspace '%s'.\n\nWelcome aboard!",
                        recipientName, addedBy, workspaceName))
                .entityType("WORKSPACE")
                .workspaceId(workspaceId)
                .triggeredBy(addedBy)
                .build();
        
        publishNotification(event);
    }
}
