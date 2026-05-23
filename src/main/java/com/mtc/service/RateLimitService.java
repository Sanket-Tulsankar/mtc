package com.mtc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.mtc.config.AppProperties;
import com.mtc.exception.RateLimitException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

	private static final String RATE_KEY_PREFIX = "ratelimit:msg:";

	private static final long WINDOW_SECONDS = 60L;

	private final RedisTemplate<String, Object> redisTemplate;

	private final AppProperties appProperties;

	public void checkMessageRateLimit(UUID userId) {
		if (!appProperties.getRateLimit().isEnabled())
			return;

		String key = RATE_KEY_PREFIX + userId;
		Long count = redisTemplate.opsForValue().increment(key);

		if (count != null && count == 1) {
			redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
		}

		int limit = appProperties.getRateLimit().getMessagesPerMinute();
		if (count != null && count > limit) {
			log.warn("Rate limit exceeded for user {}: {} messages in 60s", userId, count);
			throw new RateLimitException(String.format("Rate limit exceeded: max %d messages per minute", limit));
		}
	}

	public long getRemainingMessages(UUID userId) {
		String key = RATE_KEY_PREFIX + userId;
		Object val = redisTemplate.opsForValue().get(key);
		long used = val != null ? Long.parseLong(val.toString()) : 0;
		long limit = appProperties.getRateLimit().getMessagesPerMinute();
		return Math.max(0, limit - used);
	}
}
