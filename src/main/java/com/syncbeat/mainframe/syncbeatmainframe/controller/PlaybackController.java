package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaybackActionRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaybackEventDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RedisRoomDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import com.syncbeat.mainframe.syncbeatmainframe.service.PlaybackEventPublisher;
import com.syncbeat.mainframe.syncbeatmainframe.service.PlaybackRateLimiter;
import com.syncbeat.mainframe.syncbeatmainframe.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * STOMP application-destination handler for playback commands.
 * Client: SEND /app/room/{roomId}/action  ->  this method.
 * See syncbeat-service-breakdown.md "Backend API Service" > WebSocket layer, steps 4-5.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PlaybackController {
	private final RedisService redisService;
	private final PlaybackEventPublisher playbackEventPublisher;
	private final PlaybackRateLimiter rateLimiter;

	@MessageMapping("/room/{roomId}/action")
	public void handleAction(@DestinationVariable String roomId, @Payload PlaybackActionRequestDto action, Principal principal) {
		User user = resolveUser(principal);
		if (user == null || action.getType() == null) {
			return;
		}
		String userId = user.getId().toString();
		if (!rateLimiter.tryAcquire(userId)) {
			log.warn("Rate limit exceeded for user {} in room {}", userId, roomId);
			return;
		}

		RedisRoomDto room;
		try {
			room = redisService.getRoom(roomId);
		} catch (ResponseStatusException e) {
			log.warn("Playback action for unknown room {} from user {}", roomId, userId);
			return;
		}

		// Only the host may control playback — enforced here, not just hidden client-side.
		if (!userId.equals(room.getHostId())) {
			log.warn("Rejected playback action from non-host user {} in room {}", userId, roomId);
			return;
		}

		PlaybackEventDto event = PlaybackEventDto.builder()
				.roomId(roomId)
				.eventType(action.getType())
				.timestamp(System.currentTimeMillis())
				.positionMs(action.getPositionMs())
				.trackId(action.getTrackId())
				.userId(userId)
				.build();

		playbackEventPublisher.publish(event);
	}

	private User resolveUser(Principal principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof User user) {
			return user;
		}
		return null;
	}
}
