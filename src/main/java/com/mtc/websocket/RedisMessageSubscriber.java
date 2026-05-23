package com.mtc.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtc.dto.MessageDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

	private final SimpMessagingTemplate messagingTemplate;

	private final ObjectMapper objectMapper;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel());
		String body = new String(message.getBody());

		try {
			if (channel.startsWith("chat:messages:")) {
				handleChatMessage(channel, body);
			} else if (channel.equals("chat:presence")) {
				handlePresenceEvent(body);
			} else if (channel.startsWith("chat:typing:")) {
				handleTypingEvent(channel, body);
			}
		} catch (Exception e) {
			log.error("Error processing Redis message on channel {}: {}", channel, e.getMessage(), e);
		}
	}

	private void handleChatMessage(String redisChannel, String body) throws Exception {
		String channelId = redisChannel.substring("chat:messages:".length());
		MessageDto.Response response = objectMapper.readValue(body, MessageDto.Response.class);

		String stompTopic = "/topic/channels/" + channelId + "/messages";
		messagingTemplate.convertAndSend(stompTopic, response);
		log.debug("Forwarded message to STOMP topic: {}", stompTopic);
	}

	private void handlePresenceEvent(String body) throws Exception {
		MessageDto.PresenceEvent event = objectMapper.readValue(body, MessageDto.PresenceEvent.class);
		messagingTemplate.convertAndSend("/topic/presence", event);
	}

	private void handleTypingEvent(String redisChannel, String body) throws Exception {
		String channelId = redisChannel.substring("chat:typing:".length());
		MessageDto.TypingEvent event = objectMapper.readValue(body, MessageDto.TypingEvent.class);
		messagingTemplate.convertAndSend("/topic/channels/" + channelId + "/typing", event);
	}
}
