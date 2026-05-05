package com.app.taskmanagement.notification.consumer;

import com.app.taskmanagement.notification.config.RabbitMQConfig;
import com.app.taskmanagement.notification.dto.NotificationEvent;
import com.app.taskmanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

	private final NotificationService notificationService;

	@RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
	public void consumeNotification(NotificationEvent event) {
		log.info("Received notification event from queue: {}", event.getEventType());
		try {
			notificationService.processNotification(event);
		} catch (Exception e) {
			log.error("Error processing notification event", e);
			throw e; // Re-throw to trigger DLQ
		}
	}
}