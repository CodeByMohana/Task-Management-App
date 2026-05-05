package com.app.taskmanagement.card.scheduler;

import com.app.taskmanagement.card.entity.Card;
import com.app.taskmanagement.card.repository.CardRepository;
import com.app.taskmanagement.card.messaging.NotificationPublisher;
import com.app.taskmanagement.card.messaging.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardDeadlineScheduler {

    private final CardRepository cardRepository;
    private final NotificationPublisher notificationPublisher;

    /**
     * Runs every day at 08:00 AM server time.
     * Checks for cards that are overdue, due today, or due tomorrow
     * and sends a reminder to the assigned user.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkCardDeadlines() {
        log.info("Running daily card deadline check...");
        
        // Find cards due today, tomorrow, or already overdue
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Card> cardsDueSoon = cardRepository.findOverdueOrDueSoon(tomorrow);
        
        for (Card card : cardsDueSoon) {
            if (card.getAssigneeUserId() != null) {
                String message;
                String subject;
                
                if (card.getDueDate().isBefore(LocalDate.now())) {
                    subject = "Overdue Card Alert";
                    message = String.format("The card '%s' is OVERDUE! It was due on %s.", card.getTitle(), card.getDueDate());
                } else if (card.getDueDate().isEqual(LocalDate.now())) {
                    subject = "Card Due Today";
                    message = String.format("The card '%s' is due TODAY.", card.getTitle());
                } else {
                    subject = "Card Due Tomorrow";
                    message = String.format("The card '%s' is due tomorrow (%s).", card.getTitle(), card.getDueDate());
                }

                NotificationEvent event = NotificationEvent.builder()
                        .eventType("CARD_DEADLINE")
                        .recipientUserId(card.getAssigneeUserId())
                        .subject(subject)
                        .message(message)
                        .entityId((long) card.getCardId())
                        .entityType("CARD")
                        .workspaceId(String.valueOf(card.getWorkspaceId()))
                        .triggeredByUserId(null) // System-triggered notification
                        .build();
                        
                notificationPublisher.publishNotification(event);
            }
        }
        
        log.info("Finished daily deadline check. Pushed notifications for {} cards.", cardsDueSoon.size());
    }
}
