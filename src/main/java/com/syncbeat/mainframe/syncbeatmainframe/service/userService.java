package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.AuthRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import com.syncbeat.mainframe.syncbeatmainframe.repository.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

		User user = User.builder()
				.firstName(requestDto.getFirstName())
				.lastName(requestDto.getLastName())
				.email(requestDto.getEmail())
				.password(encodedPassword)
				.admin(requestDto.getAdmin() != null ?
						requestDto.getAdmin() : false)
				.active(requestDto.getActive() != null ?
						requestDto.getActive() : true)
				.build();

		User savedUser = userRepository.save(user);
		return UserResponseDto.fromEntity(savedUser);
	}

	public List<UserResponseDto> getAllUsers() {
		return userRepository.findAll().stream()
				.map(UserResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	public UserResponseDto getUserById(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));
		return UserResponseDto.fromEntity(user);
	}

	public UserResponseDto updateUser(UUID id, UserRequestDto requestDto) {
		User existingUser = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));

		if (requestDto.getFirstName() != null) {
			existingUser.setFirstName(requestDto.getFirstName());
		}
		if (requestDto.getLastName() != null) {
			existingUser.setLastName(requestDto.getLastName());
		}
		if (requestDto.getEmail() != null && !requestDto.getEmail().isEmpty() &&
				existingUser.getEmail() != null && !existingUser.getEmail().equals(requestDto.getEmail())) {
			if (userRepository.existsByEmail(requestDto.getEmail())) {
				throw new IllegalArgumentException("User with email " + requestDto.getEmail() + " already exists");
			}
			existingUser.setEmail(requestDto.getEmail());
		}
		if (requestDto.getPassword() != null && !requestDto.getPassword().isEmpty()) {
			existingUser.setPassword(bCryptPasswordEncoder.encode(requestDto.getPassword()));
		}

		User updatedUser = userRepository.save(existingUser);
		return UserResponseDto.fromEntity(updatedUser);
	}

	public void deleteUser(UUID id) {
		if (!userRepository.existsById(id)) {
			throw new RuntimeException("User not found with id: " + id);
		}
		userRepository.deleteById(id);
	}

	public boolean checkUserExistsByMail(@Email @NotBlank String email) {
		return userRepository.existsByEmail(email);
	}

	public void verifyUserCredentials(AuthRequestDto authRequestDto) {
		userRepository.findByEmail(authRequestDto.getEmail())
				.filter(user -> bCryptPasswordEncoder.matches(authRequestDto.getPassword(), user.getPassword()))
				.orElseThrow(() -> new RuntimeException("Invalid email or password"));
	}

	public UserResponseDto getUserByEmail(@Email @NotBlank String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found with email: " + email));
		return UserResponseDto.fromEntity(user);
	}

	public boolean checkIfAdmin(UUID uuid) {
		User user = userRepository.findById(uuid)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + uuid));
		return user.getAdmin() != null && user.getAdmin();
	}
}
