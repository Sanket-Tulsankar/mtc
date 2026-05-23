package com.mtc.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtc.dto.MessageDto;
import com.mtc.security.UserPrincipal;
import com.mtc.service.MessageService;
import com.mtc.service.PresenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

	private final MessageService messageService;

	private final PresenceService presenceService;

	private final RedisTemplate<String, Object> redisTemplate;

	private final ObjectMapper objectMapper;


	@MessageMapping("/workspaces/{workspaceId}/channels/{channelId}/send")
	public void sendMessage(@DestinationVariable UUID workspaceId, @DestinationVariable UUID channelId,
			@Payload MessageDto.SendRequest request, SimpMessageHeaderAccessor headerAccessor) {

		UserPrincipal principal = extractPrincipal(headerAccessor);
		if (principal == null)
			return;

		try {
			MessageDto.Response response = messageService.sendMessage(workspaceId, channelId, request,
					principal.getId());

			MessageDto.AckEvent ack = MessageDto.AckEvent.builder().messageId(response.getId())
					.clientMsgId(request.getClientMsgId()).sequenceNum(response.getSequenceNum()).status("DELIVERED")
					.build();

			String userDest = "/user/" + principal.getId() + "/queue/acks";
			redisTemplate.convertAndSend("chat:ack:" + principal.getId(), objectMapper.writeValueAsString(ack));

		} catch (Exception e) {
			log.error("Error handling WebSocket send: channelId={}, user={}", channelId, principal.getId(), e);
			sendErrorToUser(principal.getId(), e.getMessage(), headerAccessor);
		}
	}

	@MessageMapping("/workspaces/{workspaceId}/channels/{channelId}/typing")
	public void typing(@DestinationVariable UUID workspaceId, @DestinationVariable UUID channelId,
			@Payload TypingPayload payload, SimpMessageHeaderAccessor headerAccessor) {

		UserPrincipal principal = extractPrincipal(headerAccessor);
		if (principal == null)
			return;

		MessageDto.TypingEvent event = MessageDto.TypingEvent.builder().channelId(channelId).userId(principal.getId())
				.username(principal.getUsername()).typing(payload.isTyping()).build();

		try {
			redisTemplate.convertAndSend("chat:typing:" + channelId, objectMapper.writeValueAsString(event));
		} catch (JsonProcessingException e) {
			log.error("Failed to publish typing event", e);
		}
	}

	@MessageMapping("/channels/{channelId}/reconnect")
	public void reconnect(@DestinationVariable UUID channelId, @Payload MessageDto.ReconnectRequest request,
			SimpMessageHeaderAccessor headerAccessor) {

		UserPrincipal principal = extractPrincipal(headerAccessor);
		if (principal == null)
			return;

		List<MessageDto.Response> missed = messageService.getMissedMessages(channelId, request.getLastSeqNum(),
				principal.getId());

		if (!missed.isEmpty()) {
			log.info("Delivering {} missed messages to user {} for channel {}", missed.size(), principal.getId(),
					channelId);
			missed.forEach(msg -> redisTemplate.convertAndSend("/topic/channels/" + channelId + "/messages", msg));
		}

		presenceService.heartbeat(principal.getId());
	}

	@MessageMapping("/heartbeat")
	public void heartbeat(SimpMessageHeaderAccessor headerAccessor) {
		UserPrincipal principal = extractPrincipal(headerAccessor);
		if (principal != null) {
			presenceService.heartbeat(principal.getId());
		}
	}

	private UserPrincipal extractPrincipal(SimpMessageHeaderAccessor accessor) {
		Authentication auth = (Authentication) accessor.getUser();
		if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal))
			return null;
		return (UserPrincipal) auth.getPrincipal();
	}

	private void sendErrorToUser(UUID userId, String message, SimpMessageHeaderAccessor accessor) {
		log.warn("Sending error to user {}: {}", userId, message);
	}

	@lombok.Data
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	public static class TypingPayload {
		private boolean typing;
	}
}