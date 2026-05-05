package com.app.taskmanagement.notification.repository;

import com.app.taskmanagement.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);

	List<Notification> findByStatusAndCreatedAtBefore(Notification.NotificationStatus status, LocalDateTime dateTime);

	long countByRecipientEmailAndStatus(String recipientEmail, Notification.NotificationStatus status);

	long countByRecipientEmailAndIsRead(String recipientEmail, Boolean isRead);

	@Modifying
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientEmail = :email AND n.isRead = false")
	void markAllAsReadByEmail(@Param("email") String email);
}