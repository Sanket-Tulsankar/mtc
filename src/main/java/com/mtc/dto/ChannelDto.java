package com.mtc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import com.mtc.entity.Channel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChannelDto {

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CreateRequest {
		@NotBlank
		@Size(min = 1, max = 100)
		@Pattern(regexp = "^[a-z0-9-_]+$", message = "Channel name must be lowercase alphanumeric with hyphens/underscores")
		private String name;

		private String description;

		private boolean privateChannel = false;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Response {

		private UUID id;

		private UUID workspaceId;

		private String name;

		private String description;

		private boolean privateChannel;

		private int memberCount;

		private Instant createdAt;

		private boolean archived;

		public static Response from(Channel c) {
			return Response.builder().id(c.getId()).workspaceId(c.getWorkspace().getId()).name(c.getName())
					.description(c.getDescription()).privateChannel(c.isPrivateChannel())
					.memberCount(c.getMembers().size()).createdAt(c.getCreatedAt()).archived(c.isArchived()).build();
		}
	}
}
