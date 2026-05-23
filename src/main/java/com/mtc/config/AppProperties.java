package com.mtc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private final Jwt jwt = new Jwt();
	private final RateLimit rateLimit = new RateLimit();
	private final Redis redis = new Redis();

	@Data
	public static class Jwt {
		private String secret;
		private long expiration;
		private long refreshExpiration;
	}

	@Data
	public static class RateLimit {
		private int messagesPerMinute = 60;
		private boolean enabled = true;
	}

	@Data
	public static class Redis {
		private int presenceTtl = 60;
		private int typingTtl = 5;
		private int retryQueueTtl = 86400;
	}
}
