package com.app.taskmanagement.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an attachment linked to a card.
 *
 * --------------------------- 📦 STORAGE DESIGN (METADATA-ONLY)
 * ---------------------------
 *
 * This service follows a metadata-based storage approach:
 *
 * - Actual file content (binary data) is NOT stored in this service. - Files
 * are stored externally (e.g., AWS S3 or CDN). - This entity stores only
 * metadata: fileName, fileUrl, fileType, fileSize
 *
 * This pattern improves: - Performance (no large file handling in service) -
 * Scalability (cloud storage handles file distribution) - Maintainability
 * (separation of concerns)
 *
 * --------------------------- 🔄 FILE UPLOAD FLOW ---------------------------
 *
 * 1. Client requests a pre-signed upload URL from a storage service. 2. Client
 * uploads file directly to S3 using that URL. 3. S3 returns a public/private
 * file URL. 4. Client calls this service API with metadata. 5. This entity is
 * persisted in the database.
 *
 * This avoids routing file data through backend services.
 *
 * --------------------------- 🔗 MICROSERVICE PRINCIPLE
 * ---------------------------
 *
 * - cardId is stored as a primitive (Integer). - No JPA relationship with Card
 * entity.
 *
 * Reason: - Card belongs to another service (card-service). - Microservices
 * must maintain database isolation. - Communication happens via APIs, not
 * joins.
 */

@Entity
@Table(name = "attachments", indexes = {

		// Optimizes queries like: "fetch all attachments by cardId"
		@Index(name = "idx_attachment_card_id", columnList = "card_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

	/**
	 * Primary key (auto-incremented).
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer attachmentId;

	/**
	 * Foreign reference to Card (stored as ID only).
	 *
	 * Note: - Not a foreign key constraint across services - No @ManyToOne mapping
	 * used
	 */
	@Column(name = "card_id", nullable = false)
	private Integer cardId;

	/**
	 * ID of the user who uploaded the file.
	 *
	 * Used for: - Authorization (only uploader can delete) - Auditing
	 */
	@Column(nullable = false)
	private Integer uploaderUserId;

	/**
	 * Original file name provided by the user.
	 *
	 * Used in: - UI display - Download filename
	 */
	@Column(nullable = false)
	private String fileName;

	/**
	 * Fully qualified URL of the file in external storage.
	 *
	 * Example: https://cdn.app.com/attachments/xyz.png
	 *
	 * Used by frontend to: - Render previews - Trigger downloads
	 */
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String fileUrl;

	/**
	 * MIME type of the file.
	 *
	 * Examples: image/png, application/pdf
	 *
	 * Helps client determine rendering strategy.
	 */
	@Column(nullable = false)
	private String fileType;

	/**
	 * File size in kilobytes (KB).
	 *
	 * Used for: - UI display - Future validations (e.g., max file size limits)
	 */
	@Column(nullable = false)
	private Long fileSizeKb;

	/**
	 * Timestamp when the attachment was created.
	 *
	 * - Automatically populated by Hibernate - Immutable (not updated after
	 * creation)
	 */
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime uploadedAt;
}