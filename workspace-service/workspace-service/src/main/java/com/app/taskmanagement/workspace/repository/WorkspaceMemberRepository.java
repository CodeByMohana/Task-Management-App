package com.app.taskmanagement.workspace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.taskmanagement.workspace.entity.WorkspaceMember;

import java.util.Optional;

/**
 * Data access layer for WorkspaceMember. Handles queries related to who is a
 * member of which workspace.
 */
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Integer> {

	/**
	 * Find a specific membership record — is this user in this workspace? Returns
	 * Optional because the user might NOT be a member.
	 */
	Optional<WorkspaceMember> findByWorkspaceWorkspaceIdAndUserId(int workspaceId, int userId);

	/**
	 * Get all members of a given workspace.
	 */
	List<WorkspaceMember> findAllByWorkspaceWorkspaceId(int workspaceId);

	/**
	 * Check if a user is already a member — used before adding someone.
	 */
	boolean existsByWorkspaceWorkspaceIdAndUserId(int workspaceId, int userId);

	/**
	 * Remove a user from a workspace.
	 */
	void deleteByWorkspaceWorkspaceIdAndUserId(int workspaceId, int userId);
}