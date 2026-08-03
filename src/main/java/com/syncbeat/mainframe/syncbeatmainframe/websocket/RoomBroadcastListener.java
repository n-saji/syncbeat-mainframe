package com.syncbeat.mainframe.syncbeatmainframe.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub -> STOMP fan-out. On a message for channel room:{roomId}
 * (published by syncbeat-sync's redis_client.publish_state), forward the raw
 * payload verbatim to /topic/room/{roomId} for every locally-subscribed session.
 * See syncbeat-service-breakdown.md "Backend API Service" > WebSocket layer, step 6.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomBroadcastListener implements MessageListener {
	private static final String CHANNEL_PREFIX = "room:";

	private final SimpMessagingTemplate messagingTemplate;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		if (!channel.startsWith(CHANNEL_PREFIX)) {
			return;
		}
		String roomId = channel.substring(CHANNEL_PREFIX.length());
		String payload = new String(message.getBody(), StandardCharsets.UTF_8);
		messagingTemplate.convertAndSend("/topic/room/" + roomId, payload);
	}
}
