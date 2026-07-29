package com.syncbeat.mainframe.syncbeatmainframe.service;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RedisRoomDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisService {
	private final StringRedisTemplate redisTemplate;
	private static final String ROOM_HASH = "room:%s:state";
	private static final String ROOM_MEMBERS = "room:%s:members";
	private static final Duration ROOM_TTL = Duration.ofHours(6);

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
			connection.hGetAll(hashKey(roomId).getBytes());
			connection.sMembers(membersKey(roomId).getBytes());
			return null;
		});

		@SuppressWarnings("unchecked")
		Map<Object, Object> hash = (Map<Object, Object>) results.get(0);
		if (hash == null || hash.isEmpty()) {
			throw new RuntimeException("Room not found");
		}
		@SuppressWarnings("unchecked")
		Set<Object> members = (Set<Object>) results.get(1);
		return RedisRoomDto.fromRedis(roomId, hash, members);
	}

	public RedisRoomDto addMember(String roomId, String userId) {
		assertRoomExists(roomId);
		redisTemplate.opsForSet().add(membersKey(roomId), userId);
		return getRoom(roomId);
	}

	/** Returns true if the room is now empty (caller may want to trigger cleanup / HOST_CHANGED). */
	public boolean removeMember(String roomId, String userId) {
		redisTemplate.opsForSet().remove(membersKey(roomId), userId);
		Long remaining = redisTemplate.opsForSet().size(membersKey(roomId));
		return remaining == null || remaining == 0;
	}

	private void assertRoomExists(String roomId) {
		if (Boolean.FALSE.equals(redisTemplate.hasKey(hashKey(roomId)))) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + roomId);
		}
	}
}
