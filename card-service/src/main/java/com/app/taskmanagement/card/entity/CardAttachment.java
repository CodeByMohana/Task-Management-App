package com.app.taskmanagement.card.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Stores metadata about a file attached to a card.
 *
 * IMPORTANT: This entity stores METADATA only — not the actual file bytes. The
 * actual file is stored in AWS S3 (or local storage in dev). We store only the
 * reference (URL/key) needed to retrieve it.
 *
 * Why separate storage? - Storing files in a DB is extremely slow and expensive
 * - S3 is designed for file storage — cheap, fast, scalable - DB stores only
 * what it's good at: structured metadata
 *
 * Example record: fileName = "design-mockup.png" fileType = "image/png"
 * fileSize = 204800 (200 KB in bytes) fileUrl =
 * "https://s3.amazonaws.com/flowboard/cards/5/design-mockup.png"
 */
@Entity
@Table(name = "card_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardAttachment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer attachmentId;

	/**
	 * The card this attachment belongs to. FetchType.LAZY — only load card data
	 * when explicitly accessed.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "card_id", nullable = false)
	private Card card;

	/**
	 * Original filename as uploaded by the user. Example: "sprint-design.pdf",
	 * "bug-screenshot.png"
	 */
	@Column(nullable = false)
	private String fileName;

	/**
	 * MIME type of the file. Example: "image/png", "application/pdf", "text/plain"
	 * Used by the frontend to decide how to display/download the file.
	 */
	@Column(nullable = false)
	private String fileType;

	/**
	 * File size in bytes. Used to display human-readable size in the UI (e.g. "204
	 * KB").
	 */
	private Long fileSize;

	/**
	 * The URL or S3 key where the actual file is stored. In production: full S3 URL
	 * or CloudFront CDN URL In development: local file path or mock URL
	 */
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String fileUrl;

	/**
	 * The userId who uploaded this attachment.
	 */
	@Column(nullable = false)
	private Integer uploadedByUserId;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime uploadedAt;
}