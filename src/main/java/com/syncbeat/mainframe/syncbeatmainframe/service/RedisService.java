package com.syncbeat.mainframe.syncbeatmainframe.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RedisRoomDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
	private final StringRedisTemplate redisTemplate;
	private static final String ROOM_HASH = "room:%s:state";
	private static final String ROOM_MEMBERS = "room:%s:members";
	private static final Duration ROOM_TTL = Duration.ofHours(6);

	// See PlaybackEventPublisher for why this is a locally-owned Jackson 2 mapper rather than
	// the auto-configured Jackson 3 bean. RedisRoomDto.updatedAt is an Instant, and a bare
	// ObjectMapper doesn't know how to serialize java.time types without this registered -
	// broadcastState() was silently failing (caught, logged, swallowed) on every call without it.
	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	private String hashKey(String roomId) { return ROOM_HASH.formatted(roomId); }
	private String membersKey(String roomId) { return ROOM_MEMBERS.formatted(roomId); }

	public void saveRoom(RoomResponseDto room) {
		String roomId = room.getId().toString();
		String hostId = room.getUser().getId().toString();

		Map<String, String> fields = Map.of(
				"roomName", room.getName(),
				"roomType", room.isPublic() ? "public" : "private",
				"hostId", hostId,
				"trackId", "",
				"positionMs", "0",
				"isPlaying", "false",
				"updatedAt", Instant.now().toString()
		);

		redisTemplate.opsForHash().putAll(hashKey(roomId), fields);
		redisTemplate.opsForSet().add(membersKey(roomId), hostId);
		redisTemplate.expire(hashKey(roomId), ROOM_TTL);
		redisTemplate.expire(membersKey(roomId), ROOM_TTL);
	}

	public RedisRoomDto getRoom(String roomId) {
		List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
			connection.hashCommands().hGetAll(hashKey(roomId).getBytes());
			connection.setCommands().sMembers(membersKey(roomId).getBytes());
			return null;
		});

		@SuppressWarnings("unchecked")
		Map<Object, Object> hash = (Map<Object, Object>) results.get(0);
		if (hash == null || hash.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + roomId);
		}
		@SuppressWarnings("unchecked")
		Set<Object> members = (Set<Object>) results.get(1);
		return RedisRoomDto.fromRedis(roomId, hash, members);
	}

	public Optional<RedisRoomDto> tryGetRoom(String roomId) {
		if (Boolean.FALSE.equals(redisTemplate.hasKey(hashKey(roomId)))) {
			return Optional.empty();
		}
		return Optional.of(getRoom(roomId));
	}

	public RedisRoomDto addMember(String roomId, String userId) {
		assertRoomExists(roomId);
		redisTemplate.opsForSet().add(membersKey(roomId), userId);
		return getRoom(roomId);
	}

	// removeMember tears down a room's live state entirely once its last member leaves
	// (see below) - so a room that emptied out and is now being rejoined has nothing in
	// Redis to add a member to. Reseed it from the Postgres row (the source of truth for
	// room_id/name/type/host) exactly like a fresh saveRoom, but only if it's actually
	// missing - never stomp on a still-live room's track/position/is_playing.
	public void ensureRoomState(RoomResponseDto room) {
		if (Boolean.TRUE.equals(redisTemplate.hasKey(hashKey(room.getId().toString())))) {
			return;
		}
		saveRoom(room);
	}

	public void removeMember(String roomId, String userId) {
		String hash = hashKey(roomId);
		String membersKey = membersKey(roomId);

		redisTemplate.opsForSet().remove(membersKey, userId);
		Set<String> remaining = redisTemplate.opsForSet().members(membersKey);

		if (remaining == null || remaining.isEmpty()) {
			redisTemplate.delete(hash);
			redisTemplate.delete(membersKey);
			return;
		}

		Object currentHostId = redisTemplate.opsForHash().get(hash, "hostId");
		if (userId.equals(currentHostId)) {
			String newHostId = remaining.iterator().next();
			redisTemplate.opsForHash().put(hash, "hostId", newHostId);
			redisTemplate.opsForHash().put(hash, "updatedAt", Instant.now().toString());
			// Per the design doc this should be a HOST_CHANGED event through SNS/SQS/sync-service
			// like every other state transition; it's a direct Redis write + broadcast instead,
			// which is out of step with that architecture but the pragmatic fix for the actual bug:
			// without this, already-connected clients never learn the host changed until some
			// unrelated action happens to re-broadcast state, so a live host badge would go stale.
			broadcastState(roomId);
		}
	}

	private void broadcastState(String roomId) {
		try {
			RedisRoomDto state = getRoom(roomId);
			redisTemplate.convertAndSend("room:" + roomId, objectMapper.writeValueAsString(state));
		} catch (Exception e) {
			log.warn("Failed to broadcast state for room {}", roomId, e);
		}
	}

	private void assertRoomExists(String roomId) {
		if (Boolean.FALSE.equals(redisTemplate.hasKey(hashKey(roomId)))) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + roomId);
		}
	}
}
