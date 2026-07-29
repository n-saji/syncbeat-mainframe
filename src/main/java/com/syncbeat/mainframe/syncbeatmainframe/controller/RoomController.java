package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomCreationRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
	private final RoomService roomService;

	@PostMapping("/create")
	public ResponseEntity<RoomResponseDto> createRoom(
	                                                 @RequestBody RoomCreationRequestDto roomCreationRequestDto) {
		RoomResponseDto createdRoom = roomService.createRoom(roomCreationRequestDto);
		return ResponseEntity.ok(createdRoom);
	}

	@PutMapping("/update/{id}")
	public ResponseEntity<RoomResponseDto> updateRoom(
			@PathVariable String id,
	                                                 @Valid @RequestBody RoomRequestDto roomRequestDto) {
		RoomResponseDto updatedRoom = roomService.updateRoom(id,roomRequestDto);
		return ResponseEntity.ok(updatedRoom);
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<Void> deleteRoom(
	                                        @PathVariable String id) {
		roomService.deleteRoom(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/user/{uid}/all")
	public ResponseEntity<List<RoomResponseDto>> getAllUserRooms(@PathVariable String uid) {
		List<RoomResponseDto> rooms = roomService.getAllUserRooms(uid);
		return ResponseEntity.ok(rooms);
	}

	@GetMapping("/my-rooms")
	public ResponseEntity<List<RoomResponseDto>> getMyRooms() {
		List<RoomResponseDto> rooms = roomService.getMyRooms();
		return ResponseEntity.ok(rooms);
	}

	@PostMapping("/{id}/join")
	public ResponseEntity<RoomResponseDto> joinRoom(@PathVariable String id) {
		RoomResponseDto resp = roomService.joinRoom(id);
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/{id}/leave")
	public ResponseEntity<Void> leaveRoom(@PathVariable String id) {
		roomService.leaveRoom(id);
		return ResponseEntity.noContent().build();
	}
}
