package com.app.taskmanagement.workspace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a user's membership inside a workspace.
 *
 * Instead of a plain join table (user_id + workspace_id), we use an entity
 * because we need to store extra data: the member's ROLE in this workspace.
 *
 * Example: User A is an ADMIN in "Engineering Workspace", but only a MEMBER in
 * "Marketing Workspace".
 */
@Entity
@Table(name = "workspace_members",
		// Composite unique constraint: one user can only be added to a workspace once
		uniqueConstraints = @UniqueConstraint(columnNames = { "workspace_id", "user_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	/**
	 * The workspace this membership belongs to. FetchType.LAZY = don't load
	 * workspace data unless explicitly accessed (performance).
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	/**
	 * The ID of the user who is a member. We store just the ID — not a User object
	 * — because User is in auth-service.
	 */
	@Column(nullable = false)
	private Integer userId;

	/**
	 * Role of this user inside the workspace. ADMIN can manage members and
	 * settings. MEMBER can create boards and cards.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Role role = Role.MEMBER;

	/**
	 * When this user joined the workspace.
	 */
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime joinedAt;

	// ─── Enum ────────────────────────────────────────────────────────────────

	public enum Role {
		ADMIN, // can manage members, update/delete workspace
		MEMBER // can create boards and cards
	}
}
