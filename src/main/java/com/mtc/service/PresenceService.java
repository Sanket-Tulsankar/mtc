package com.mtc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.mtc.config.AppProperties;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final String PRESENCE_KEY_PREFIX = "presence:";
    
    private static final String ONLINE_SET_KEY = "online_users";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final AppProperties appProperties;

    public void markOnline(UUID userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(
                key,
                Instant.now().toString(),
                appProperties.getRedis().getPresenceTtl(),
                TimeUnit.SECONDS);
        stringRedisTemplate.opsForSet().add(ONLINE_SET_KEY, userId.toString());
        log.debug("User {} is now online", userId);
    }

    public void markOffline(UUID userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        stringRedisTemplate.delete(key);
        stringRedisTemplate.opsForSet().remove(ONLINE_SET_KEY, userId.toString());
        log.debug("User {} is now offline", userId);
    }

    public boolean isOnline(UUID userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    public Instant getLastSeen(UUID userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        String val = stringRedisTemplate.opsForValue().get(key);
        return val != null ? Instant.parse(val) : null;
    }

    public void heartbeat(UUID userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        stringRedisTemplate.expire(key, appProperties.getRedis().getPresenceTtl(), TimeUnit.SECONDS);
    }

    public Set<UUID> getOnlineUsers() {
        Set<String> members = stringRedisTemplate.opsForSet().members(ONLINE_SET_KEY);
        if (members == null) return java.util.Collections.emptySet();
        return members.stream().map(UUID::fromString).collect(Collectors.toSet());
    }
}
