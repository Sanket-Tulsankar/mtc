package com.mtc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mtc.entity.Message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class MessageDto {

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SendRequest {
		@NotBlank
		@Size(max = 4000)
		private String content;

		private String clientMsgId;

		private UUID replyToId;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Response {
		private UUID id;
		private UUID channelId;
		private UUID workspaceId;
		private UserDto user;
		private String content;
		private String messageType;
		private Long sequenceNum;
		private String clientMsgId;
		private UUID replyToId;
		private boolean deleted;
		private Instant createdAt;
		private Instant updatedAt;

		public static Response from(Message m) {
			return Response.builder().id(m.getId()).channelId(m.getChannel().getId()).workspaceId(m.getWorkspaceId())
					.user(UserDto.from(m.getUser())).content(m.isDeleted() ? "[deleted]" : m.getContent())
					.messageType(m.getMessageType().name()).sequenceNum(m.getSequenceNum())
					.clientMsgId(m.getClientMsgId()).replyToId(m.getReplyTo() != null ? m.getReplyTo().getId() : null)
					.deleted(m.isDeleted()).createdAt(m.getCreatedAt()).updatedAt(m.getUpdatedAt()).build();
		}
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TypingEvent {
		private String type = "TYPING";
		private UUID channelId;
		private UUID userId;
		private String username;
		private boolean typing;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PresenceEvent {
		private String type = "PRESENCE";
		private UUID userId;
		private String username;
		private boolean online;
		private Instant lastSeen;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AckEvent {
		private String type = "ACK";
		private UUID messageId;
		private String clientMsgId;
		private Long sequenceNum;
		private String status; 
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PagedResponse {
		private java.util.List<Response> messages;
		private long totalElements;
		private int totalPages;
		private int page;
		private int size;
		private boolean hasMore;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ReconnectRequest {
		private Long lastSeqNum;
		private UUID channelId;
	}
}
