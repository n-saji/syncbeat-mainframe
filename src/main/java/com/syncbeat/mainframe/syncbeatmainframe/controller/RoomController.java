package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
	private final RoomService roomService;

	@PostMapping("/create")
	public ResponseEntity<RoomResponseDto> createRoom(
	                                                 @RequestBody RoomRequestDto roomRequestDto) {
		RoomResponseDto createdRoom = roomService.createRoom( roomRequestDto);
		return ResponseEntity.ok(createdRoom);
	}
}
