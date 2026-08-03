package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.RedisRoomDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomCreationRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.Room;
import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import com.syncbeat.mainframe.syncbeatmainframe.repository.RoomRepository;
import com.syncbeat.mainframe.syncbeatmainframe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {
	private final RoomRepository roomRepository;
	private final UserRepository userRepository;
	private final RedisService redisService;

	public RoomResponseDto createRoom(
	                                  RoomCreationRequestDto roomCreationRequestDto) {

		RoomResponseDto resp =
				RoomResponseDto.fromEntity(roomRepository.save(Room.builder()
				.name(roomCreationRequestDto.getName())
				.active(true)
				.isPublic(roomCreationRequestDto.isPublic())
				.createdBy(getCurrentUser())
				.build()));

		redisService.saveRoom(resp);
		return resp;
	}

	public RoomResponseDto getRoom(String roomId) {
		UUID id ;
		try{
			id = UUID.fromString(roomId);
		}catch(IllegalArgumentException e){
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid room id");
		}
		Room room = roomRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

		// Attach live redis state when there's an active listening session; a room
		// can legitimately exist in Postgres with no live state (never started, or
		// its redis TTL expired), so this is best-effort, not a failure.
		RedisRoomDto state = redisService.tryGetRoom(roomId).orElse(null);
		RoomResponseDto resp = RoomResponseDto.fromEntity(room, state);
		resp.setCurrentHost(resolveHost(state));
		return resp;
	}

	// state.host_id can point at a different user than room.created_by once a host
	// re-election has happened (see RedisService.removeMember) - resolve it here rather
	// than making every caller do this UUID lookup themselves.
	private UserResponseDto resolveHost(RedisRoomDto state) {
		if (state == null || state.getHostId() == null) return null;
		try {
			return userRepository.findById(UUID.fromString(state.getHostId()))
					.map(UserResponseDto::fromEntity)
					.orElse(null);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public RoomResponseDto updateRoom(String roomId,
	                                  RoomRequestDto roomRequestDto) {

		UUID id;
		try{
			id = UUID.fromString(roomId);
		}catch(Exception e){
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid room id");
		}
		Room existingRoom =
				roomRepository.findById(id)
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

		if(!existingRoom.getCreatedBy().getId().equals(getCurrentUser().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to update this room");
		}
		if (roomRequestDto.getName() != null && !roomRequestDto.getName().equals(existingRoom.getName())) {
			existingRoom.setName(roomRequestDto.getName());
		}
		if(roomRequestDto.getIsActive() != null  && roomRequestDto.getIsActive() != existingRoom.isActive()) {
			existingRoom.setActive(roomRequestDto.getIsActive());
		}
		if(roomRequestDto.getIsPublic() != null && roomRequestDto.getIsPublic() != existingRoom.isPublic()) {
			existingRoom.setPublic(roomRequestDto.getIsPublic());
		}
		return RoomResponseDto.fromEntity(roomRepository.save(existingRoom));
	}

	private User getCurrentUser() {
		return (User) Objects.requireNonNull(SecurityContextHolder
						.getContext()
						.getAuthentication())
				.getPrincipal();
	}

	public void deleteRoom(String roomId) {
		UUID roomID ;
		try{
			roomID = UUID.fromString(roomId);
		}catch(Exception e){
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid room id");
		}
		Room existingRoom =
				roomRepository.findById(roomID)
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
		if(!existingRoom.getCreatedBy().getId().equals(getCurrentUser().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this room");
		}
		roomRepository.delete(existingRoom);
	}

	public List<RoomResponseDto> getAllUserRooms(String userId) {
		try {
			UUID uuid = UUID.fromString(userId);
			User user = userRepository.findById(uuid)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
			return roomRepository.findByCreatedBy(user).stream()
					.map(RoomResponseDto::fromEntity)
					.toList();
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid UUID string: " + userId);
		}
	}

	public List<RoomResponseDto> getMyRooms() {
		return roomRepository.findByCreatedByAndActive(getCurrentUser(), true).stream()
				.map(RoomResponseDto::fromEntity)
				.toList();
	}

	public List<RoomResponseDto> getPublicRooms() {
		return roomRepository.findDiscoverablePublicRooms(getCurrentUser()).stream()
				.map(room -> {
					RedisRoomDto state = redisService.tryGetRoom(room.getId().toString()).orElse(null);
					RoomResponseDto resp = RoomResponseDto.fromEntity(room, state);
					resp.setCurrentHost(resolveHost(state));
					return resp;
				})
				.toList();
	}

	public RoomResponseDto joinRoom(String roomId) {

		try {
			UUID roomID;
			roomID = UUID.fromString(roomId);
			Room room = roomRepository.findById(roomID)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

			// A room that emptied out (last member's WS disconnect tore down its Redis
			// state) has nothing for addMember to attach to - reseed it from Postgres
			// before joining, rather than 404ing on a room that legitimately still exists.
			redisService.ensureRoomState(RoomResponseDto.fromEntity(room));

			RedisRoomDto state = redisService.addMember(roomId,
					getCurrentUser().getId().toString());
			RoomResponseDto resp = RoomResponseDto.fromEntity(room, state);
			resp.setCurrentHost(resolveHost(state));
			return resp;
		}
		catch(IllegalArgumentException e){
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid room id");
		}

	}

	public void leaveRoom(String roomId) {
		// Removes the user from the redis member set; RedisService also handles
		// host re-election and cleaning up the room state if it's now empty.
		redisService.removeMember(roomId, getCurrentUser().getId().toString());
	}
}
