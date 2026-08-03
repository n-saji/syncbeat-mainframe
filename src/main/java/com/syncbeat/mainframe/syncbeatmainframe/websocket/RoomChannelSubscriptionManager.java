package com.syncbeat.mainframe.syncbeatmainframe.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks, per this instance, how many local STOMP sessions are subscribed to each
 * room and (un)registers the Redis Pub/Sub listener for room:{roomId} accordingly —
 * subscribe once per room per instance, not once per user.
 * See syncbeat-service-breakdown.md "Backend API Service" > WebSocket layer, steps 3 & 7.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomChannelSubscriptionManager {
	private final RedisMessageListenerContainer listenerContainer;
	private final RoomBroadcastListener broadcastListener;

	private final Map<String, Integer> localSubscriberCounts = new ConcurrentHashMap<>();

	public synchronized void subscribe(String roomId) {
		int count = localSubscriberCounts.merge(roomId, 1, Integer::sum);
		if (count == 1) {
			listenerContainer.addMessageListener(broadcastListener, channelTopic(roomId));
			log.debug("Subscribed instance to redis channel room:{}", roomId);
		}
	}

	public synchronized void unsubscribe(String roomId) {
		Integer count = localSubscriberCounts.computeIfPresent(roomId, (id, current) -> current - 1);
		if (count != null && count <= 0) {
			localSubscriberCounts.remove(roomId);
			listenerContainer.removeMessageListener(broadcastListener, channelTopic(roomId));
			log.debug("Unsubscribed instance from redis channel room:{}", roomId);
		}
	}

	private ChannelTopic channelTopic(String roomId) {
		return new ChannelTopic("room:" + roomId);
	}

	// Rooms this instance currently has at least one local STOMP subscriber for - the
	// SYNC_PULSE scheduler uses this to only pulse rooms someone is actually listening to
	// (syncbeat-service-breakdown.md, "the sync-pulse specifically").
	public Set<String> activeRoomIds() {
		return Set.copyOf(localSubscriberCounts.keySet());
	}
}
