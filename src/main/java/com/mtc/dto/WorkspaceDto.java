package com.mtc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import com.mtc.entity.Workspace;
import com.mtc.entity.WorkspaceMember;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class WorkspaceDto {

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CreateRequest {
		@NotBlank
		@Size(min = 2, max = 100)
		private String name;

		@NotBlank
		@Size(min = 2, max = 50)
		@Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens")
		private String slug;

		private String description;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Response {
		private UUID id;
		private String name;
		private String slug;
		private String description;
		private int memberCount;
		private Instant createdAt;

		public static Response from(Workspace w) {
			return Response.builder().id(w.getId()).name(w.getName()).slug(w.getSlug()).description(w.getDescription())
					.memberCount(w.getMembers().size()).createdAt(w.getCreatedAt()).build();
		}
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MemberResponse {
		private UUID userId;
		private String username;
		private String displayName;
		private String avatarUrl;
		private String role;
		private boolean online;
		private Instant joinedAt;

		public static MemberResponse from(WorkspaceMember m) {
			return MemberResponse.builder().userId(m.getUser().getId()).username(m.getUser().getUsername())
					.displayName(m.getUser().getDisplayName() != null ? m.getUser().getDisplayName()
							: m.getUser().getUsername())
					.avatarUrl(m.getUser().getAvatarUrl()).role(m.getRole().name()).joinedAt(m.getJoinedAt()).build();
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class InviteRequest {
		@NotBlank
		private String email;
	}
}
