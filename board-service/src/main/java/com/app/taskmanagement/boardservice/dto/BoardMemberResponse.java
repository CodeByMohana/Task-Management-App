package com.app.taskmanagement.boardservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.app.taskmanagement.boardservice.entity.BoardMember;

/**
 * Represents one member in the board's member list response.
 */
@Data
@Builder
public class BoardMemberResponse {

	private Integer id;
	private Integer userId;
	private String role;
	private LocalDateTime joinedAt;

	public static BoardMemberResponse from(BoardMember member) {
		return BoardMemberResponse.builder().id(member.getId()).userId(member.getUserId()).role(member.getRole().name())
				.joinedAt(member.getJoinedAt()).build();
	}
}