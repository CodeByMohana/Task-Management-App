package com.app.taskmanagement.card.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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
        log.info("Published notification event: {} to userId: {}", event.getEventType(), event.getRecipientUserId());
    }
    
    public void notifyCardAssigned(Integer recipientUserId, String cardTitle, Integer assignedByUserId, Long cardId, String workspaceId, String boardName, String workspaceName) {
        String message;
        if (boardName != null && workspaceName != null) {
            message = String.format("You have been assigned to the card '%s' in board '%s' and workspace '%s'.\n\nCheck it out now!", cardTitle, boardName, workspaceName);
        } else {
            message = String.format("You have been assigned to the card '%s'.\n\nCheck it out now!", cardTitle);
        }

        NotificationEvent event = NotificationEvent.builder()
                .eventType("CARD_ASSIGNED")
                .recipientUserId(recipientUserId)
                .subject("You've been assigned to a card")
                .message(message)
                .entityId(cardId)
                .entityType("CARD")
                .workspaceId(workspaceId)
                .triggeredByUserId(assignedByUserId)
                .build();
        publishNotification(event);
    }
}
