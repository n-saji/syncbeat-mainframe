package com.syncbeat.mainframe.syncbeatmainframe.websocket;

import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import com.syncbeat.mainframe.syncbeatmainframe.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the "on subscribe start listening, on disconnect clean up" lifecycle from
 * syncbeat-service-breakdown.md "Backend API Service" > WebSocket layer, steps 3 & 7:
 *  - SUBSCRIBE /topic/room/{roomId}  -> ref-count in RoomChannelSubscriptionManager
 *  - UNSUBSCRIBE / DISCONNECT        -> drop the ref-count; on disconnect also remove
 *                                       the user from Redis room membership.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSubscriptionEventListener {
	private static final Pattern ROOM_TOPIC_PATTERN = Pattern.compile("^/topic/room/([^/]+)$");

	private final RoomChannelSubscriptionManager channelSubscriptionManager;
	private final RedisService redisService;

	// sessionId -> (subscriptionId -> roomId), so UNSUBSCRIBE/DISCONNECT (which only
	// carry subscriptionId/sessionId, not destination) can resolve back to a roomId.
	private final Map<String, Map<String, String>> sessionSubscriptions = new ConcurrentHashMap<>();

	@EventListener
	public void handleSubscribe(SessionSubscribeEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String destination = accessor.getDestination();
		String sessionId = accessor.getSessionId();
		String subscriptionId = accessor.getSubscriptionId();
		if (destination == null || sessionId == null || subscriptionId == null) {
			return;
		}

		Matcher matcher = ROOM_TOPIC_PATTERN.matcher(destination);
		if (!matcher.matches()) {
			return;
		}
		String roomId = matcher.group(1);

		sessionSubscriptions
				.computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>())
				.put(subscriptionId, roomId);
		channelSubscriptionManager.subscribe(roomId);
	}

	@EventListener
	public void handleUnsubscribe(SessionUnsubscribeEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = accessor.getSessionId();
		String subscriptionId = accessor.getSubscriptionId();
		if (sessionId == null || subscriptionId == null) {
			return;
		}

		Map<String, String> subs = sessionSubscriptions.get(sessionId);
		if (subs == null) {
			return;
		}
		String roomId = subs.remove(subscriptionId);
		if (roomId != null) {
			channelSubscriptionManager.unsubscribe(roomId);
		}
	}

	@EventListener
	public void handleDisconnect(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = accessor.getSessionId();
		if (sessionId == null) {
			return;
		}

		Map<String, String> subs = sessionSubscriptions.remove(sessionId);
		if (subs == null || subs.isEmpty()) {
			return;
		}

		String userId = resolveUserId(event.getUser());
		for (String roomId : subs.values()) {
			channelSubscriptionManager.unsubscribe(roomId);
			if (userId != null) {
				removeMemberSafely(roomId, userId);
			}
		}
	}

	private void removeMemberSafely(String roomId, String userId) {
		try {
			redisService.removeMember(roomId, userId);
		} catch (Exception e) {
			log.warn("Failed to remove member {} from room {} on disconnect", userId, roomId, e);
		}
	}

	private String resolveUserId(Principal principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof User user) {
			return user.getId().toString();
		}
		return null;
	}
}
