package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.UserRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.Users;
import com.syncbeat.mainframe.syncbeatmainframe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	public UserResponseDto createUser(UserRequestDto requestDto) {
		if (requestDto.getEmail() != null && userRepository.existsByEmail(requestDto.getEmail())) {
			throw new IllegalArgumentException("User with email " + requestDto.getEmail() + " already exists");
		}

		String encodedPassword = null;
		if (requestDto.getPassword() != null && !requestDto.getPassword().isEmpty()) {
			encodedPassword = bCryptPasswordEncoder.encode(requestDto.getPassword());
		}

		Users user = Users.builder()
				.f_name(requestDto.getF_name())
				.l_name(requestDto.getL_name())
				.email(requestDto.getEmail())
				.password(encodedPassword)
				.is_admin(requestDto.getIs_admin() != null ? requestDto.getIs_admin() : false)
				.is_active(requestDto.getIs_active() != null ? requestDto.getIs_active() : true)
				.build();

		Users savedUser = userRepository.save(user);
		return UserResponseDto.fromEntity(savedUser);
	}

	public List<UserResponseDto> getAllUsers() {
		return userRepository.findAll().stream()
				.map(UserResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	public UserResponseDto getUserById(UUID id) {
		Users user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));
		return UserResponseDto.fromEntity(user);
	}

	public UserResponseDto updateUser(UUID id, UserRequestDto requestDto) {
		Users existingUser = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));

		if (requestDto.getF_name() != null) {
			existingUser.setF_name(requestDto.getF_name());
		}
		if (requestDto.getL_name() != null) {
			existingUser.setL_name(requestDto.getL_name());
		}
		if (requestDto.getEmail() != null) {
			existingUser.setEmail(requestDto.getEmail());
		}
		if (requestDto.getPassword() != null && !requestDto.getPassword().isEmpty()) {
			existingUser.setPassword(bCryptPasswordEncoder.encode(requestDto.getPassword()));
		}
		if (requestDto.getIs_admin() != null) {
			existingUser.setIs_admin(requestDto.getIs_admin());
		}
		if (requestDto.getIs_active() != null) {
			existingUser.setIs_active(requestDto.getIs_active());
		}

		Users updatedUser = userRepository.save(existingUser);
		return UserResponseDto.fromEntity(updatedUser);
	}

	public void deleteUser(UUID id) {
		if (!userRepository.existsById(id)) {
			throw new RuntimeException("User not found with id: " + id);
		}
		userRepository.deleteById(id);
	}
}
