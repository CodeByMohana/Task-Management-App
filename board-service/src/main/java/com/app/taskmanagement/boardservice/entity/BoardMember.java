package com.app.taskmanagement.boardservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a user's membership on a specific Board.
 *
 * Board-level roles are separate from workspace-level roles. Example: User A →
 * workspace ADMIN, but board OBSERVER (read-only on this board) User B →
 * workspace MEMBER, but board ADMIN (manages this specific board)
 *
 * Three board roles: ADMIN → full control: edit board, manage members, delete
 * MEMBER → create/edit cards, move cards, add comments OBSERVER → read-only
 * access, cannot make changes
 */
@Entity
@Table(name = "board_members", uniqueConstraints = @UniqueConstraint(columnNames = { "board_id", "user_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	/**
	 * The board this membership belongs to. FetchType.LAZY = only load board data
	 * when explicitly accessed.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "board_id", nullable = false)
	private Board board;

	/**
	 * The userId of the member — stored as plain ID, not a User entity. User data
	 * lives in auth-service, not this DB.
	 */
	@Column(nullable = false)
	private Integer userId;

	/**
	 * The user's role on this specific board.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Role role = Role.MEMBER;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime joinedAt;

	// ─── Enum ────────────────────────────────────────────────────────────────

	public enum Role {
		ADMIN, // manage board settings and members
		MEMBER, // create and edit cards
		OBSERVER // read-only
	}
}
