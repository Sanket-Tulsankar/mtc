package com.mtc.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtc.dto.MessageDto;
import com.mtc.security.UserPrincipal;
import com.mtc.service.PresenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

	private final PresenceService presenceService;

	private final RedisTemplate<String, Object> redisTemplate;

	private final ObjectMapper objectMapper;

	@EventListener
	public void handleWebSocketConnect(SessionConnectedEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		UserPrincipal principal = extractPrincipal(accessor);
		if (principal == null)
			return;

		UUID userId = principal.getId();
		presenceService.markOnline(userId);

		publishPresenceEvent(userId, principal.getUsername(), true);
		log.info("WebSocket connected: userId={}, session={}", userId, accessor.getSessionId());
	}

	@EventListener
	public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		UserPrincipal principal = extractPrincipal(accessor);
		if (principal == null)
			return;

		UUID userId = principal.getId();
		presenceService.markOffline(userId);

		publishPresenceEvent(userId, principal.getUsername(), false);
		log.info("WebSocket disconnected: userId={}, session={}", userId, accessor.getSessionId());
	}

	private void publishPresenceEvent(UUID userId, String username, boolean online) {
		MessageDto.PresenceEvent event = MessageDto.PresenceEvent.builder().userId(userId).username(username)
				.online(online).lastSeen(Instant.now()).build();
		try {
			redisTemplate.convertAndSend("chat:presence", objectMapper.writeValueAsString(event));
		} catch (JsonProcessingException e) {
			log.error("Failed to publish presence event", e);
		}
	}

	private UserPrincipal extractPrincipal(StompHeaderAccessor accessor) {
		Authentication auth = (Authentication) accessor.getUser();
		if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal))
			return null;
		return (UserPrincipal) auth.getPrincipal();
	}
}