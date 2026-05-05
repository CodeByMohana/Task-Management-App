package com.app.taskmanagement.comment.messaging;

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
    
    public void notifyCommentAdded(Integer recipientUserId, String cardTitle, String commentText, Integer commenterUserId, Long commentId) {
        NotificationEvent event = NotificationEvent.builder()
                .eventType("COMMENT_ADDED")
                .recipientUserId(recipientUserId)
                .subject("New comment on a card")
                .message(String.format("Someone commented on '%s':\n\n\"%s\"", cardTitle, commentText))
                .entityId(commentId)
                .entityType("COMMENT")
                .triggeredByUserId(commenterUserId)
                .build();
        publishNotification(event);
    }
}
