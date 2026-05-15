package com.app.taskmanagement.workspace.messaging;

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
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                event
            );
            log.info("Published notification event: {} to userId: {}", event.getEventType(), event.getRecipientUserId());
        } catch (Exception ex) {
            log.warn("Notification publish failed for event {} to userId {}. Continuing without notification.",
                    event.getEventType(), event.getRecipientUserId(), ex);
        }
    }
    
    public void notifyWorkspaceMemberAdded(Integer recipientUserId, String workspaceName, Integer addedByUserId, Integer workspaceId) {
        NotificationEvent event = NotificationEvent.builder()
                .eventType("WORKSPACE_MEMBER_ADDED")
                .recipientUserId(recipientUserId)
                .subject("You've been added to a workspace")
                .message(String.format("You have been added to the workspace '%s'.\n\nWelcome aboard!", workspaceName))
                .entityType("WORKSPACE")
                .workspaceId(String.valueOf(workspaceId))
                .triggeredByUserId(addedByUserId)
                .build();
        publishNotification(event);
    }
}
