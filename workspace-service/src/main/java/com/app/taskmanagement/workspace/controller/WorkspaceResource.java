package com.app.taskmanagement.workspace.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.app.taskmanagement.workspace.dto.*;
import com.app.taskmanagement.workspace.service.WorkspaceService;

import java.util.List;

/**
 * REST Controller for workspace operations.
 *
 * @RestController = @Controller + @ResponseBody (auto-converts return values to
 *                 JSON)
 * @RequestMapping sets the base URL for all endpoints in this class.
 *
 *                 How @AuthenticationPrincipal works here: In
 *                 JwtAuthenticationFilter, we set the principal to the userId
 *                 (Integer). So @AuthenticationPrincipal Integer userId gives
 *                 us the logged-in user's ID directly in the method parameter —
 *                 no need to manually parse the JWT again.
 */
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspace", description = "Workspace management endpoints")
public class WorkspaceResource {

	private final WorkspaceService workspaceService;

	// =========================================================================
	// WORKSPACE CRUD
	// =========================================================================

	/**
	 * POST /api/workspaces Create a new workspace. The logged-in user becomes the
	 * first ADMIN.
	 *
	 * @Valid triggers bean validation on the request body (@NotBlank, @Size etc.)
	 *        Returns 201 Created with the new workspace in the response body.
	 */
	@PostMapping
	@Operation(summary = "Create a new workspace", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<WorkspaceResponse> createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request,
			@AuthenticationPrincipal Integer userId) {

		WorkspaceResponse response = workspaceService.createWorkspace(request, userId);
		// 201 Created is more accurate than 200 OK when a resource is created
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * GET /api/workspaces/{workspaceId} Get a single workspace by ID. Private
	 * workspaces are only accessible to their members.
	 */
	@GetMapping("/{workspaceId}")
	@Operation(summary = "Get workspace by ID", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<WorkspaceResponse> getWorkspace(@PathVariable int workspaceId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(workspaceService.getWorkspace(workspaceId, userId));
	}

	/**
	 * GET /api/workspaces/my Get all workspaces the logged-in user is a member of.
	 * This is what populates the sidebar in the UI.
	 */
	@GetMapping("/my")
	@Operation(summary = "Get my workspaces", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces(@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(workspaceService.getMyWorkspaces(userId));
	}

	/**
	 * GET /api/workspaces/public Get all public workspaces — accessible without
	 * login (permitted in SecurityConfig). Useful for a discovery/browse page.
	 */
	@GetMapping("/public")
	@Operation(summary = "Browse all public workspaces")
	public ResponseEntity<List<WorkspaceResponse>> getPublicWorkspaces() {
		return ResponseEntity.ok(workspaceService.getPublicWorkspaces());
	}

	/**
	 * PUT /api/workspaces/{workspaceId} Update workspace name, description, or
	 * visibility. Only workspace ADMINs can do this.
	 */
	@PutMapping("/{workspaceId}")
	@Operation(summary = "Update workspace", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<WorkspaceResponse> updateWorkspace(@PathVariable int workspaceId,
			@Valid @RequestBody UpdateWorkspaceRequest request, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(workspaceService.updateWorkspace(workspaceId, request, userId));
	}

	/**
	 * DELETE /api/workspaces/{workspaceId} Permanently delete a workspace. Only the
	 * workspace creator can do this.
	 *
	 * Returns 204 No Content — success with no response body (standard for DELETE).
	 */
	@DeleteMapping("/{workspaceId}")
	@Operation(summary = "Delete workspace", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> deleteWorkspace(@PathVariable int workspaceId,
			@AuthenticationPrincipal Integer userId) {

		workspaceService.deleteWorkspace(workspaceId, userId);
		return ResponseEntity.noContent().build(); // 204 No Content
	}

	// =========================================================================
	// MEMBER MANAGEMENT
	// =========================================================================

	/**
	 * GET /api/workspaces/{workspaceId}/members Get all members of a workspace.
	 * Only members of the workspace can see this.
	 */
	@GetMapping("/{workspaceId}/members")
	@Operation(summary = "Get workspace members", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<List<WorkspaceMemberResponse>> getMembers(@PathVariable int workspaceId,
			@AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(workspaceService.getMembers(workspaceId, userId));
	}

	/**
	 * POST /api/workspaces/{workspaceId}/members Add a user to the workspace. Only
	 * workspace ADMINs can do this.
	 *
	 * Request body: { "userId": 5, "role": "MEMBER" }
	 */
	@PostMapping("/{workspaceId}/members")
	@Operation(summary = "Add member to workspace", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<WorkspaceMemberResponse> addMember(@PathVariable int workspaceId,
			@Valid @RequestBody AddMemberRequest request, @AuthenticationPrincipal Integer userId) {

		WorkspaceMemberResponse response = workspaceService.addMember(workspaceId, request, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * DELETE /api/workspaces/{workspaceId}/members/{targetUserId} Remove a user
	 * from the workspace.
	 *
	 * Rules enforced in service layer: - Members can remove themselves (leave) -
	 * ADMINs can remove anyone (but not the last admin)
	 */
	@DeleteMapping("/{workspaceId}/members/{targetUserId}")
	@Operation(summary = "Remove member from workspace", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<Void> removeMember(@PathVariable int workspaceId, @PathVariable int targetUserId,
			@AuthenticationPrincipal Integer userId) {

		workspaceService.removeMember(workspaceId, targetUserId, userId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * PATCH /api/workspaces/{workspaceId}/members/{targetUserId}/role Change a
	 * member's role (MEMBER ↔ ADMIN). Only workspace ADMINs can do this.
	 *
	 * We use @RequestParam here instead of a request body because it's a single
	 * small value — simpler than wrapping it in a JSON object.
	 *
	 * Example: PATCH /api/workspaces/1/members/5/role?role=ADMIN
	 */
	@PatchMapping("/{workspaceId}/members/{targetUserId}/role")
	@Operation(summary = "Update member role", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<WorkspaceMemberResponse> updateMemberRole(@PathVariable int workspaceId,
			@PathVariable int targetUserId, @RequestParam String role, @AuthenticationPrincipal Integer userId) {

		return ResponseEntity.ok(workspaceService.updateMemberRole(workspaceId, targetUserId, role, userId));
	}
}