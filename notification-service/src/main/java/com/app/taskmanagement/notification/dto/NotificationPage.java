package com.app.taskmanagement.notification.dto;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.app.taskmanagement.notification.entity.Notification;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//Create a serializable wrapper
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPage implements Serializable {
	private List<Notification> content;
	private int pageNumber;
	private int pageSize;
	private long totalElements;
	private int totalPages;

	public static NotificationPage from(Page<Notification> page) {
		return new NotificationPage(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}

	public Page<Notification> toPage() {
		return new PageImpl<>(content, PageRequest.of(pageNumber, pageSize), totalElements);
	}
}