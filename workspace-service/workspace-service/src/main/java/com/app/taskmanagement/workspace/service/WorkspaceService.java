package com.app.taskmanagement.workspace.service;

import java.util.List;

import com.app.taskmanagement.workspace.dto.*;

/**
 * Contract (interface) for workspace business logic.
 *
 * Why use an interface? - Separates WHAT the service does from HOW it does it -
 * Makes it easy to swap implementations (e.g. for testing with mocks) - Spring
 * injects the correct implementation (@Service) automatically
 */
public interface WorkspaceService {

	/**
	 * Create a new workspace. The calling user becomes the first ADMIN member.
	 *
	 * @param request       workspace details from client
	 * @param creatorUserId userId from JWT (who is creating it)
	 */
	WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, int creatorUserId);

	/**
	 * Get a single workspace by ID. Validates that the requesting user has access
	 * (is a member, or workspace is public).
	 */
	WorkspaceResponse getWorkspace(int workspaceId, int requestingUserId);

	/**
	 * Get all workspaces that the requesting user is a member of.
	 */
	List<WorkspaceResponse> getMyWorkspaces(int userId);

	/**
	 * Get all PUBLIC workspaces — for browsing/discovery.
	 */
	List<WorkspaceResponse> getPublicWorkspaces();

	/**
	 * Update workspace name, description, or visibility. Only workspace ADMINs can
	 * do this.
	 */
	WorkspaceResponse updateWorkspace(int workspaceId, UpdateWorkspaceRequest request, int requestingUserId);

	/**
	 * Permanently delete a workspace and all its data. Only the workspace creator
	 * or Platform Admin can do this.
	 */
	void deleteWorkspace(int workspaceId, int requestingUserId);

	/**
	 * Add a user to a workspace with a given role. Only workspace ADMINs can add
	 * members.
	 */
	WorkspaceMemberResponse addMember(int workspaceId, AddMemberRequest request, int requestingUserId);

	/**
	 * Remove a user from a workspace. ADMINs can remove anyone. Members can remove
	 * themselves (leave).
	 */
	void removeMember(int workspaceId, int targetUserId, int requestingUserId);

	/**
	 * Change a member's role (MEMBER ↔ ADMIN). Only workspace ADMINs can do this.
	 */
	WorkspaceMemberResponse updateMemberRole(int workspaceId, int targetUserId, String newRole, int requestingUserId);

	/**
	 * Get all members of a workspace.
	 */
	List<WorkspaceMemberResponse> getMembers(int workspaceId, int requestingUserId);
}