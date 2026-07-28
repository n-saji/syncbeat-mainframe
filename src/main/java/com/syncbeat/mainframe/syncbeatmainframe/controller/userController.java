package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.syncbeat.mainframe.syncbeatmainframe.dto.UserRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import com.syncbeat.mainframe.syncbeatmainframe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
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
		User user = (User) Objects.requireNonNull(SecurityContextHolder
						.getContext()
						.getAuthentication())
				.getPrincipal();

		if(user != null && !user.getAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		List<UserResponseDto> users = userService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
		User user = (User) Objects.requireNonNull(SecurityContextHolder
						.getContext()
						.getAuthentication())
				.getPrincipal();

		if(user != null && !user.getAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		UserResponseDto userResp = userService.getUserById(id);
		return ResponseEntity.ok(userResp);
	}

	@PutMapping("/update")
	public ResponseEntity<UserResponseDto> updateUser(@RequestBody UserRequestDto requestDto) {
		UserResponseDto updatedUser = userService.updateUser(requestDto);
		return ResponseEntity.ok(updatedUser);
	}

	@DeleteMapping("/delete")
	public ResponseEntity<Void> deleteUser() {
		User user = (User) Objects.requireNonNull(SecurityContextHolder
						.getContext()
						.getAuthentication())
				.getPrincipal();
		assert user != null;
		userService.deleteUser(user.getId());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponseDto> getCurrentUser() {
		User user = (User) Objects.requireNonNull(SecurityContextHolder
						.getContext()
						.getAuthentication())
				.getPrincipal();
		assert user != null;
		return ResponseEntity.ok(UserResponseDto.fromEntity(user));
	}
}
