package com.app.taskmanagement.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a comment on a card (like in Trello).
 *
 * --------------------------- 💬 HOW COMMENTS WORK HERE
 * ---------------------------
 *
 * We support simple comment threads:
 *
 * 1. A normal comment → has no parent (parentCommentId = null) 2. A reply →
 * points to another comment using parentCommentId
 *
 * Example:
 *
 * Comment (id=1) ├── Reply (parentCommentId = 1) ├── Reply (parentCommentId =
 * 1)
 *
 * IMPORTANT: We only allow 2 levels: Comment → Reply ❌ Reply → Reply (not
 * allowed)
 *
 * Why? - Keeps UI simple - Easier to fetch from DB - Avoids complex nested
 * structures
 *
 * --------------------------- 🗑️ SOFT DELETE (IMPORTANT)
 * ---------------------------
 *
 * When a comment is deleted: - We DO NOT remove it from DB - We just mark it as
 * deleted = true - And clear its content
 *
 * Why? - Replies should not lose their parent - Keeps conversation structure
 * intact - Useful for audit/history
 *
 * --------------------------- 🔗 MICROSERVICE DESIGN NOTE
 * ---------------------------
 *
 * We only store IDs (not full objects):
 *
 * - cardId → comes from card-service - authorUserId → comes from auth-service
 *
 * Why? - Each service has its own database - No direct table joins across
 * services
 */

@Entity
@Table(name = "comments", indexes = {

		// Used when fetching all comments of a card
		@Index(name = "idx_comment_card_id", columnList = "card_id"),

		// Used when fetching replies of a comment
		@Index(name = "idx_comment_parent_id", columnList = "parent_comment_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

	// Unique ID of the comment
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer commentId;

	/**
	 * ID of the card this comment belongs to. (Actual card data is in card-service)
	 */
	@Column(name = "card_id", nullable = false)
	private Integer cardId;

	/**
	 * ID of the user who wrote the comment. (Actual user data is in auth-service)
	 */
	@Column(nullable = false)
	private Integer authorUserId;

	/**
	 * The text content of the comment.
	 *
	 * - Stored as TEXT so it can be long - Can include things like @mentions
	 *
	 * When deleted: → content becomes empty "" → UI can show "[deleted]"
	 */
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	/**
	 * This is the key field for threading.
	 *
	 * - null → this is a top-level comment - not null → this is a reply to another
	 * comment
	 */
	@Column(name = "parent_comment_id")
	private Integer parentCommentId;

	/**
	 * Soft delete flag:
	 *
	 * false → normal comment true → deleted comment
	 *
	 * We don't remove comments from DB to: - keep reply structure intact - maintain
	 * history
	 */
	@Column(nullable = false)
	@Builder.Default
	private boolean deleted = false;

	/**
	 * List of replies to this comment.
	 *
	 * Example: If this comment has ID = 1, this list contains all comments where
	 * parent_comment_id = 1
	 *
	 * Notes: - LAZY → replies are loaded only when needed - No cascade delete →
	 * replies handled separately
	 */
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_comment_id", insertable = false, updatable = false)
	@Builder.Default
	private List<Comment> replies = new ArrayList<>();

	// Automatically set when record is created
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	// Automatically updated when record changes
	@UpdateTimestamp
	private LocalDateTime updatedAt;
}