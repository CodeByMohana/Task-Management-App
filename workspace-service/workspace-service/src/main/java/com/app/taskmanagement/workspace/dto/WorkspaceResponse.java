package com.app.taskmanagement.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.app.taskmanagement.workspace.entity.Workspace;

/**
 * What we return to the client after workspace operations. Never return the raw
 * entity — always use a response DTO. This way you control exactly what data
 * leaves your API.
 */
@Data
@Builder
public class WorkspaceResponse {

	private Integer workspaceId;
	private String name;
	private String description;
	private String visibility;
	private Integer createdByUserId;
	private int memberCount;
	private List<WorkspaceMemberResponse> members;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	/**
	 * Static factory method — converts a Workspace entity to a WorkspaceResponse
	 * DTO. Keeping this logic in the DTO keeps the service layer clean.
	 *
	 * @param workspace      the entity from the database
	 * @param includeMembers whether to include the full member list (avoid when
	 *                       listing many workspaces)
	 */
	public static WorkspaceResponse from(Workspace workspace, boolean includeMembers) {
		return WorkspaceResponse.builder().workspaceId(workspace.getWorkspaceId()).name(workspace.getName())
				.description(workspace.getDescription()).visibility(workspace.getVisibility().name())
				.createdByUserId(workspace.getCreatedByUserId()).memberCount(workspace.getMembers().size())
				.members(includeMembers ? workspace.getMembers().stream().map(WorkspaceMemberResponse::from).toList()
						: null)
				.createdAt(workspace.getCreatedAt()).updatedAt(workspace.getUpdatedAt()).build();
	}
}