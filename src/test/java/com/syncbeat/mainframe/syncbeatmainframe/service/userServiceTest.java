package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.UserRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.users;
import com.syncbeat.mainframe.syncbeatmainframe.repository.userRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class userServiceTest {

	@Mock
	private userRepository repository;

	@Mock
	private BCryptPasswordEncoder passwordEncoder;

	@InjectMocks
	private userService service;

	private UUID userId;
	private users userEntity;
	private UserRequestDto requestDto;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		userEntity = users.builder()
				.id(userId)
				.f_name("Alice")
				.l_name("Smith")
				.email("alice@example.com")
				.password("encoded_pass")
				.is_admin(false)
				.is_active(true)
				.created_at(LocalDateTime.now())
				.updated_at(LocalDateTime.now())
				.build();

		requestDto = UserRequestDto.builder()
				.f_name("Alice")
				.l_name("Smith")
				.email("alice@example.com")
				.password("plain_pass")
				.is_admin(false)
				.is_active(true)
				.build();
	}

	@Test
	@DisplayName("Create User - Should encode password using BCryptPasswordEncoder and save user")
	void testCreateUser_Success() {
		when(repository.existsByEmail("alice@example.com")).thenReturn(false);
		when(passwordEncoder.encode("plain_pass")).thenReturn("encoded_pass");
		when(repository.save(any(users.class))).thenReturn(userEntity);

		UserResponseDto response = service.createUser(requestDto);

		assertNotNull(response);
		assertEquals("Alice", response.getF_name());
		assertEquals("alice@example.com", response.getEmail());

		verify(passwordEncoder, times(1)).encode("plain_pass");
		verify(repository, times(1)).save(any(users.class));
	}

	@Test
	@DisplayName("Create User - Should throw exception if email already exists")
	void testCreateUser_DuplicateEmail() {
		when(repository.existsByEmail("alice@example.com")).thenReturn(true);

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> service.createUser(requestDto)
		);

		assertTrue(exception.getMessage().contains("already exists"));
		verify(repository, never()).save(any(users.class));
	}

	@Test
	@DisplayName("Get All Users - Should return list of UserResponseDto")
	void testGetAllUsers() {
		when(repository.findAll()).thenReturn(List.of(userEntity));

		List<UserResponseDto> usersList = service.getAllUsers();

		assertEquals(1, usersList.size());
		assertEquals("Alice", usersList.get(0).getF_name());
		verify(repository, times(1)).findAll();
	}

	@Test
	@DisplayName("Get User By ID - Should return user when found")
	void testGetUserById_Success() {
		when(repository.findById(userId)).thenReturn(Optional.of(userEntity));

		UserResponseDto response = service.getUserById(userId);

		assertNotNull(response);
		assertEquals(userId, response.getId());
		assertEquals("Alice", response.getF_name());
	}

	@Test
	@DisplayName("Get User By ID - Should throw exception when user not found")
	void testGetUserById_NotFound() {
		when(repository.findById(userId)).thenReturn(Optional.empty());

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> service.getUserById(userId)
		);

		assertTrue(exception.getMessage().contains("User not found"));
	}

	@Test
	@DisplayName("Update User - Should update user fields and encode new password if provided")
	void testUpdateUser_Success() {
		UserRequestDto updateDto = UserRequestDto.builder()
				.f_name("Alice Updated")
				.password("new_plain_pass")
				.build();

		when(repository.findById(userId)).thenReturn(Optional.of(userEntity));
		when(passwordEncoder.encode("new_plain_pass")).thenReturn("new_encoded_pass");
		when(repository.save(any(users.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponseDto updatedResponse = service.updateUser(userId, updateDto);

		assertNotNull(updatedResponse);
		assertEquals("Alice Updated", updatedResponse.getF_name());
		verify(passwordEncoder, times(1)).encode("new_plain_pass");
		verify(repository, times(1)).save(userEntity);
	}

	@Test
	@DisplayName("Delete User - Should delete when user exists")
	void testDeleteUser_Success() {
		when(repository.existsById(userId)).thenReturn(true);
		doNothing().when(repository).deleteById(userId);

		assertDoesNotThrow(() -> service.deleteUser(userId));
		verify(repository, times(1)).deleteById(userId);
	}

	@Test
	@DisplayName("Delete User - Should throw exception when user does not exist")
	void testDeleteUser_NotFound() {
		when(repository.existsById(userId)).thenReturn(false);

		assertThrows(RuntimeException.class, () -> service.deleteUser(userId));
		verify(repository, never()).deleteById(any());
	}
}
