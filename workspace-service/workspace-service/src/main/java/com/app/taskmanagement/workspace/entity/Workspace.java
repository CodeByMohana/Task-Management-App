package com.app.taskmanagement.workspace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Workspace — the top-level container in FlowBoard. A workspace
 * groups related boards and team members together.
 *
 * Example: "My Team's Workspace" can contain boards like "Sprint 1", "Bug
 * Tracker".
 */
@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workspace {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer workspaceId;

	/**
	 * The display name of the workspace. e.g. "Engineering Team", "Marketing Q2"
	 */
	@Column(nullable = false)
	private String name;

	/**
	 * Optional description to explain what this workspace is for.
	 */
	private String description;

	/**
	 * Visibility: PUBLIC workspaces can be discovered by any user. PRIVATE
	 * workspaces are invite-only.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Visibility visibility = Visibility.PRIVATE;

	/**
	 * The userId of the user who created this workspace. We store just the ID (not
	 * a User entity) because User lives in auth-service DB. This is a key
	 * microservices pattern — services don't share tables.
	 */
	@Column(nullable = false)
	private Integer createdByUserId;

	/**
	 * All members of this workspace. CascadeType.ALL means if workspace is deleted,
	 * its members are deleted too. orphanRemoval = true ensures members removed
	 * from list are deleted from DB.
	 */
	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<WorkspaceMember> members = new ArrayList<>();

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	// ─── Enum ────────────────────────────────────────────────────────────────

	public enum Visibility {
		PUBLIC, // visible to all users
		PRIVATE // invite-only
	}
}