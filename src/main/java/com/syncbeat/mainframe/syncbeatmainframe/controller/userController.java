package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.syncbeat.mainframe.syncbeatmainframe.dto.UserRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping("/create")
	public ResponseEntity<UserResponseDto> createUser(@RequestBody UserRequestDto requestDto) {
		UserResponseDto createdUser = userService.createUser(requestDto);
		return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
	}

	@GetMapping("/all")
	public ResponseEntity<List<UserResponseDto>> getAllUsers() {
		List<UserResponseDto> users = userService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
		UserResponseDto user = userService.getUserById(id);
		return ResponseEntity.ok(user);
	}

	@PutMapping("/{id}")
	public ResponseEntity<UserResponseDto> updateUser(
			@PathVariable UUID id,
			@RequestBody UserRequestDto requestDto) {
		UserResponseDto updatedUser = userService.updateUser(id, requestDto);
		return ResponseEntity.ok(updatedUser);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}
}
