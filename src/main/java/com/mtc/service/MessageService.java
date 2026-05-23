package com.mtc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtc.dto.MessageDto;
import com.mtc.entity.Channel;
import com.mtc.entity.Message;
import com.mtc.entity.User;
import com.mtc.exception.ForbiddenException;
import com.mtc.exception.ResourceNotFoundException;
import com.mtc.repository.ChannelMemberRepository;
import com.mtc.repository.MessageRepository;
import com.mtc.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

	private static final String SEQ_KEY_PREFIX = "channel:seq:";

	private static final String RETRY_QUEUE_PREFIX = "retry:";

	private final MessageRepository messageRepository;

	private final ChannelMemberRepository channelMemberRepository;

	private final UserRepository userRepository;

	private final ChannelService channelService;

	private final RateLimitService rateLimitService;

	private final RedisTemplate<String, Object> redisTemplate;

	private final SimpMessagingTemplate messagingTemplate;

	private final ObjectMapper objectMapper;

	@Transactional
	public MessageDto.Response sendMessage(UUID workspaceId, UUID channelId, MessageDto.SendRequest request,
			UUID senderId) {
		rateLimitService.checkMessageRateLimit(senderId);

		channelService.getChannelInWorkspace(channelId, workspaceId);
		channelService.verifyChannelMember(channelId, senderId);

		if (request.getClientMsgId() != null) {
			Optional<Message> existing = messageRepository.findByChannelIdAndClientMsgId(channelId,
					request.getClientMsgId());
			if (existing.isPresent()) {
				log.debug("Duplicate message suppressed: clientMsgId={}", request.getClientMsgId());
				return MessageDto.Response.from(existing.get());
			}
		}

		User sender = userRepository.findById(senderId)
				.orElseThrow(() -> new ResourceNotFoundException("User", senderId));

		Channel channel = channelService.getChannelInWorkspace(channelId, workspaceId);

		long seqNum = nextSequenceNumber(channelId);

		Message.MessageBuilder builder = Message.builder().channel(channel).workspaceId(workspaceId).user(sender)
				.content(request.getContent()).sequenceNum(seqNum).clientMsgId(request.getClientMsgId());

		if (request.getReplyToId() != null) {
			Message replyTo = messageRepository.findById(request.getReplyToId())
					.orElseThrow(() -> new ResourceNotFoundException("Message", request.getReplyToId()));
			builder.replyTo(replyTo);
		}

		Message message = messageRepository.save(builder.build());
		MessageDto.Response response = MessageDto.Response.from(message);

		publishToRedis(channelId, response);

		log.debug("Message saved: channelId={}, seq={}", channelId, seqNum);
		return response;
	}

	@Transactional(readOnly = true)
	public MessageDto.PagedResponse getHistory(UUID workspaceId, UUID channelId, UUID userId, int page, int size) {
		channelService.getChannelInWorkspace(channelId, workspaceId);
		channelService.verifyChannelMember(channelId, userId);

		Page<Message> messagePage = messageRepository.findAllByChannelIdAndDeletedFalseOrderBySequenceNumAsc(channelId,
				PageRequest.of(page, size));

		List<MessageDto.Response> messages = messagePage.getContent().stream().map(MessageDto.Response::from)
				.collect(Collectors.toList());

		return MessageDto.PagedResponse.builder().messages(messages).totalElements(messagePage.getTotalElements())
				.totalPages(messagePage.getTotalPages()).page(page).size(size).hasMore(messagePage.hasNext()).build();
	}

	@Transactional(readOnly = true)
	public List<MessageDto.Response> getMissedMessages(UUID channelId, Long lastSeqNum, UUID userId) {
		if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
			throw new ForbiddenException("Not a member of this channel");
		}
		return messageRepository.findMissedMessages(channelId, lastSeqNum).stream().map(MessageDto.Response::from)
				.collect(Collectors.toList());
	}

	@Transactional
	public MessageDto.Response deleteMessage(UUID workspaceId, UUID channelId, UUID messageId, UUID userId) {
		channelService.getChannelInWorkspace(channelId, workspaceId);
		Message message = messageRepository.findById(messageId)
				.orElseThrow(() -> new ResourceNotFoundException("Message", messageId));

		if (!message.getUser().getId().equals(userId)) {
			throw new ForbiddenException("Cannot delete another user's message");
		}

		message.setDeleted(true);
		message = messageRepository.save(message);
		MessageDto.Response response = MessageDto.Response.from(message);

		publishToRedis(channelId, response);
		return response;
	}

	private long nextSequenceNumber(UUID channelId) {
		String seqKey = SEQ_KEY_PREFIX + channelId;
		Long next = redisTemplate.opsForValue().increment(seqKey);
		if (next != null && next == 1) {
			Long dbMax = messageRepository.findMaxSequenceByChannelId(channelId);
			if (dbMax != null && dbMax > 0) {
				redisTemplate.opsForValue().set(seqKey, dbMax + 1);
				return dbMax + 1;
			}
		}
		return next != null ? next : 1L;
	}

	private void publishToRedis(UUID channelId, MessageDto.Response message) {
		String topic = "chat:messages:" + channelId;
		try {
			String payload = objectMapper.writeValueAsString(message);
			redisTemplate.convertAndSend(topic, payload);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize message for Redis pub/sub", e);
			pushToRetryQueue(channelId, message);
		}
	}

	private void pushToRetryQueue(UUID channelId, MessageDto.Response message) {
		String retryKey = RETRY_QUEUE_PREFIX + channelId;
		try {
			redisTemplate.opsForList().rightPush(retryKey, objectMapper.writeValueAsString(message));
			redisTemplate.expire(retryKey, 86400, TimeUnit.SECONDS);
		} catch (JsonProcessingException e) {
			log.error("Failed to push to retry queue", e);
		}
	}
}