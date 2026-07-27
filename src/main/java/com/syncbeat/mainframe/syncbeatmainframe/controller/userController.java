package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.syncbeat.mainframe.syncbeatmainframe.config.UserPrincipal;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
	public ResponseEntity<List<UserResponseDto>> getAllUsers(@AuthenticationPrincipal UserPrincipal user) {
		if(!userService.checkIfAdmin(user.userId())){
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		List<UserResponseDto> users = userService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserResponseDto> getUserById(@AuthenticationPrincipal UserPrincipal user,@PathVariable UUID id) {
		if(!userService.checkIfAdmin(user.userId())){
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		UserResponseDto userResp = userService.getUserById(id);
		return ResponseEntity.ok(userResp);
	}

	@PutMapping("/update")
	public ResponseEntity<UserResponseDto> updateUser(
			@AuthenticationPrincipal UserPrincipal user,
			@RequestBody UserRequestDto requestDto) {
		UserResponseDto updatedUser = userService.updateUser(user.userId(), requestDto);
		return ResponseEntity.ok(updatedUser);
	}

	@DeleteMapping("/delete")
	public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserPrincipal user) {
		userService.deleteUser(user.userId());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal user) {
		return ResponseEntity.ok(userService.getUserById(user.userId()));
	}
}
