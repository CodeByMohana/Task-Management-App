package com.app.taskmanagement.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.app.taskmanagement.workspace.entity.WorkspaceMember;


/**
 * Represents one member inside the workspace response. We return userId and
 * role — the client can look up the user's name from auth-service if needed
 * (microservices separation).
 */
@Data
@Builder
public class WorkspaceMemberResponse {

	private Integer memberId;
	private Integer userId;
	private String role;
	private LocalDateTime joinedAt;

	/**
	 * Static factory to convert WorkspaceMember entity → DTO.
	 */
	public static WorkspaceMemberResponse from(WorkspaceMember member) {
		return WorkspaceMemberResponse.builder().memberId(member.getId()).userId(member.getUserId())
				.role(member.getRole().name()).joinedAt(member.getJoinedAt()).build();
	}
}