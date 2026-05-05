package com.app.taskmanagement.workspace.service;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.app.taskmanagement.workspace.dto.*;
import com.app.taskmanagement.workspace.entity.*;
import com.app.taskmanagement.workspace.entity.WorkspaceMember;
import com.app.taskmanagement.workspace.exception.*;
import com.app.taskmanagement.workspace.repository.WorkspaceMemberRepository;
import com.app.taskmanagement.workspace.repository.WorkspaceRepository;
import com.app.taskmanagement.workspace.messaging.NotificationPublisher;

import java.util.List;

/**
 * Implementation of WorkspaceService. Contains all the actual business logic
 * for workspace operations.
 *
 * @Transactional on the class sets a default — all public methods run inside a
 *                DB transaction. This means: if something fails halfway, the
 *                entire operation is rolled back (atomic).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceServiceImpl implements WorkspaceService {

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository memberRepository;
	private final NotificationPublisher notificationPublisher;

	// ─── Create ───────────────────────────────────────────────────────────────

	@Override
	public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, int creatorUserId) {
		// Build the workspace entity from the request data
		Workspace workspace = Workspace.builder().name(request.getName()).description(request.getDescription())
				.visibility(request.getVisibility()).createdByUserId(creatorUserId).build();

		// Save the workspace first so it gets an ID assigned by the DB
		workspace = workspaceRepository.save(workspace);

		/*
		 * Automatically add the creator as an ADMIN member. Every workspace must have
		 * at least one admin. We do this immediately after creation — no separate API
		 * call needed.
		 */
		WorkspaceMember creatorMember = WorkspaceMember.builder().workspace(workspace).userId(creatorUserId)
				.role(WorkspaceMember.Role.ADMIN).build();
		memberRepository.save(creatorMember);

		// Add the member to the in-memory list so the response includes them
		workspace.getMembers().add(creatorMember);

		return WorkspaceResponse.from(workspace, true);
	}

	// ─── Read ─────────────────────────────────────────────────────────────────

	@Override
	@Transactional(readOnly = true) // readOnly = true → DB optimization, no write locks needed
	@Cacheable(value = "workspaceById", key = "#workspaceId + ':' + #requestingUserId")
	public WorkspaceResponse getWorkspace(int workspaceId, int requestingUserId) {
		Workspace workspace = findWorkspaceById(workspaceId);

		/*
		 * Access control: - PUBLIC workspaces: anyone can view - PRIVATE workspaces:
		 * only members can view
		 */
		if (workspace.getVisibility() == Workspace.Visibility.PRIVATE) {
			requireMembership(workspaceId, requestingUserId);
		}

		return WorkspaceResponse.from(workspace, true);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "myWorkspaces", key = "#userId")
	public List<WorkspaceResponse> getMyWorkspaces(int userId) {
		// Fetch all workspaces where this user is a member
		return workspaceRepository.findAllByMemberUserId(userId).stream()
				// false = don't include full member list to keep the response lightweight
				.map(w -> WorkspaceResponse.from(w, false)).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkspaceResponse> getPublicWorkspaces() {
		return workspaceRepository.findAllPublic().stream().map(w -> WorkspaceResponse.from(w, false)).toList();
	}

	// ─── Update ───────────────────────────────────────────────────────────────

	@Override
	@Caching(evict = { @CacheEvict(value = "workspaceById", allEntries = true),
			@CacheEvict(value = "myWorkspaces", allEntries = true), })
	public WorkspaceResponse updateWorkspace(int workspaceId, UpdateWorkspaceRequest request, int requestingUserId) {
		Workspace workspace = findWorkspaceById(workspaceId);

		// Only ADMINs of this workspace can update its settings
		requireAdminRole(workspaceId, requestingUserId);

		/*
		 * PATCH-style update: only update fields that were actually sent.
		 * StringUtils.hasText() returns false for null, empty string, and whitespace.
		 * This way the client can send just { "name": "New Name" } without needing to
		 * resend description and visibility.
		 */
		if (StringUtils.hasText(request.getName())) {
			workspace.setName(request.getName());
		}
		if (request.getDescription() != null) {
			workspace.setDescription(request.getDescription());
		}
		if (request.getVisibility() != null) {
			workspace.setVisibility(request.getVisibility());
		}

		return WorkspaceResponse.from(workspaceRepository.save(workspace), true);
	}

	// ─── Delete ───────────────────────────────────────────────────────────────

	@Override
	public void deleteWorkspace(int workspaceId, int requestingUserId) {
		Workspace workspace = findWorkspaceById(workspaceId);

		/*
		 * Only the original creator can delete the workspace. Even other ADMINs cannot
		 * delete it — a deliberate design choice to prevent accidental or malicious
		 * deletion by promoted members.
		 */
		if (workspace.getCreatedByUserId() != requestingUserId) {
			throw new ForbiddenException("Only the workspace creator can delete this workspace");
		}

		/*
		 * CascadeType.ALL on the members list means JPA will automatically delete all
		 * WorkspaceMember records when the workspace is deleted. Board-service will
		 * handle board cleanup separately (event-driven in production).
		 */
		workspaceRepository.delete(workspace);
	}

	// ─── Member Management ────────────────────────────────────────────────────

	@Override
	public WorkspaceMemberResponse addMember(int workspaceId, AddMemberRequest request, int requestingUserId) {
		findWorkspaceById(workspaceId); // ensure workspace exists

		// Only ADMINs can add new members
		requireAdminRole(workspaceId, requestingUserId);

		// Prevent adding someone who is already a member
		if (memberRepository.existsByWorkspaceWorkspaceIdAndUserId(workspaceId, request.getUserId())) {
			throw new DuplicateResourceException("User is already a member of this workspace");
		}

		Workspace workspaceRef = workspaceRepository.getReferenceById(workspaceId);
		WorkspaceMember member = WorkspaceMember.builder().workspace(workspaceRef).userId(request.getUserId())
				.role(request.getRole()).build();

		WorkspaceMember savedMember = memberRepository.save(member);

		// Trigger notification
		notificationPublisher.notifyWorkspaceMemberAdded(request.getUserId(), workspaceRef.getName(), requestingUserId,
				workspaceId);

		return WorkspaceMemberResponse.from(savedMember);
	}

	@Override
	public void removeMember(int workspaceId, int targetUserId, int requestingUserId) {
		findWorkspaceById(workspaceId);

		WorkspaceMember targetMember = memberRepository.findByWorkspaceWorkspaceIdAndUserId(workspaceId, targetUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Member not found in workspace"));

		boolean isSelfRemoval = (targetUserId == requestingUserId);
		boolean isAdmin = isAdmin(workspaceId, requestingUserId);

		/*
		 * Permission rules: - A member can remove THEMSELVES (leave the workspace) - An
		 * ADMIN can remove anyone EXCEPT themselves if they're the last admin - A
		 * MEMBER cannot remove others
		 */
		if (!isSelfRemoval && !isAdmin) {
			throw new ForbiddenException("Only workspace admins can remove other members");
		}

		// Prevent removing the last admin — workspace would become unmanageable
		if (targetMember.getRole() == WorkspaceMember.Role.ADMIN) {
			long adminCount = memberRepository.findAllByWorkspaceWorkspaceId(workspaceId).stream()
					.filter(m -> m.getRole() == WorkspaceMember.Role.ADMIN).count();
			if (adminCount <= 1) {
				throw new BadRequestException("Cannot remove the last admin. Promote another member first.");
			}
		}

		memberRepository.deleteByWorkspaceWorkspaceIdAndUserId(workspaceId, targetUserId);
	}

	@Override
	public WorkspaceMemberResponse updateMemberRole(int workspaceId, int targetUserId, String newRole,
			int requestingUserId) {
		findWorkspaceById(workspaceId);
		requireAdminRole(workspaceId, requestingUserId);

		WorkspaceMember member = memberRepository.findByWorkspaceWorkspaceIdAndUserId(workspaceId, targetUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Member not found in workspace"));

		// Parse the role string — throws IllegalArgumentException if invalid value
		WorkspaceMember.Role role;
		try {
			role = WorkspaceMember.Role.valueOf(newRole.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Invalid role. Must be ADMIN or MEMBER");
		}

		// Guard: don't demote the last admin
		if (member.getRole() == WorkspaceMember.Role.ADMIN && role == WorkspaceMember.Role.MEMBER) {
			long adminCount = memberRepository.findAllByWorkspaceWorkspaceId(workspaceId).stream()
					.filter(m -> m.getRole() == WorkspaceMember.Role.ADMIN).count();
			if (adminCount <= 1) {
				throw new BadRequestException("Cannot demote the last admin. Promote another member first.");
			}
		}

		member.setRole(role);
		return WorkspaceMemberResponse.from(memberRepository.save(member));
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkspaceMemberResponse> getMembers(int workspaceId, int requestingUserId) {
		findWorkspaceById(workspaceId);
		requireMembership(workspaceId, requestingUserId);

		return memberRepository.findAllByWorkspaceWorkspaceId(workspaceId).stream().map(WorkspaceMemberResponse::from)
				.toList();
	}

	// ─── Private Helper Methods ───────────────────────────────────────────────

	/**
	 * Fetches a workspace or throws 404 if it doesn't exist. Centralizing this
	 * avoids repeating the same orElseThrow in every method.
	 */
	private Workspace findWorkspaceById(int workspaceId) {
		return workspaceRepository.findById(workspaceId)
				.orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));
	}

	/**
	 * Throws 403 if the user is not a member of the workspace. Used for private
	 * workspace access control.
	 */
	private void requireMembership(int workspaceId, int userId) {
		if (!memberRepository.existsByWorkspaceWorkspaceIdAndUserId(workspaceId, userId)) {
			throw new ForbiddenException("You are not a member of this workspace");
		}
	}

	/**
	 * Throws 403 if the user is not an ADMIN of the workspace. Used before any
	 * write operation (update, delete, add/remove members).
	 */
	private void requireAdminRole(int workspaceId, int userId) {
		WorkspaceMember member = memberRepository.findByWorkspaceWorkspaceIdAndUserId(workspaceId, userId)
				.orElseThrow(() -> new ForbiddenException("You are not a member of this workspace"));

		if (member.getRole() != WorkspaceMember.Role.ADMIN) {
			throw new ForbiddenException("Only workspace admins can perform this action");
		}
	}

	/**
	 * Returns true if the user is an ADMIN in the given workspace. Used for
	 * conditional permission checks (e.g. removeMember).
	 */
	private boolean isAdmin(int workspaceId, int userId) {
		return memberRepository.findByWorkspaceWorkspaceIdAndUserId(workspaceId, userId)
				.map(m -> m.getRole() == WorkspaceMember.Role.ADMIN).orElse(false);
	}
}