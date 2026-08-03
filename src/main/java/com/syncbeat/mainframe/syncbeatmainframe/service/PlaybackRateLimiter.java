package com.syncbeat.mainframe.syncbeatmainframe.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimum-interval throttle per user so a buggy/malicious client spamming
 * playback actions (e.g. SEEK) can't flood SNS (syncbeat-service-breakdown.md,
 * "Backend API Service" > "What to be careful about").
 */
@Component
public class PlaybackRateLimiter {
	private static final long MIN_INTERVAL_MS = 150;

	private final Map<String, Long> lastActionAtMs = new ConcurrentHashMap<>();

	public boolean tryAcquire(String userId) {
		long now = System.currentTimeMillis();
		long[] accepted = {0};
		lastActionAtMs.compute(userId, (id, last) -> {
			if (last == null || now - last >= MIN_INTERVAL_MS) {
				accepted[0] = 1;
				return now;
			}
			return last;
		});
		return accepted[0] == 1;
	}
}
