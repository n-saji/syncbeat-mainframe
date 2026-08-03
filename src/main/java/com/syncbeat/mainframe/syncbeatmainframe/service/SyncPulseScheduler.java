package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaybackEventDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaybackEventType;
import com.syncbeat.mainframe.syncbeatmainframe.websocket.RoomChannelSubscriptionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Emits a SYNC_PULSE event every ~10s for each room this instance has a live STOMP
 * subscriber for. syncbeat-sync recomputes each room's canonical position from elapsed
 * time on receipt (see state._apply_sync_pulse) and rebroadcasts the corrected state,
 * which is what keeps already-connected clients from drifting apart over a long session.
 * See syncbeat-service-breakdown.md, "Sync Service" > "The sync-pulse specifically".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncPulseScheduler {
	private static final long PULSE_INTERVAL_MS = 10_000;

	private final RoomChannelSubscriptionManager subscriptionManager;
	private final PlaybackEventPublisher playbackEventPublisher;

	@Scheduled(fixedRate = PULSE_INTERVAL_MS)
	public void pulseActiveRooms() {
		for (String roomId : subscriptionManager.activeRoomIds()) {
			PlaybackEventDto event = PlaybackEventDto.builder()
					.roomId(roomId)
					.eventType(PlaybackEventType.SYNC_PULSE)
					.timestamp(System.currentTimeMillis())
					.build();
			playbackEventPublisher.publish(event);
		}
	}
}
