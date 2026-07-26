package com.syncbeat.mainframe.syncbeatmainframe.repository;

import com.syncbeat.mainframe.syncbeatmainframe.models.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class userRepositoryTest {

	@Mock
	private UserRepository repository;

	private Users testUser;
	private UUID userId;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		testUser = Users.builder()
				.id(userId)
				.f_name("John")
				.l_name("Doe")
				.email("john.doe@example.com")
				.password("encoded_password")
				.is_admin(false)
				.is_active(true)
				.build();
	}

	@Test
	@DisplayName("Should save user successfully")
	void testSaveUser() {
		when(repository.save(any(Users.class))).thenReturn(testUser);

		Users savedUser = repository.save(testUser);

		assertNotNull(savedUser);
		assertEquals("John", savedUser.getF_name());
		assertEquals("john.doe@example.com", savedUser.getEmail());
		verify(repository, times(1)).save(testUser);
	}

	@Test
	@DisplayName("Should find user by id")
	void testFindById() {
		when(repository.findById(userId)).thenReturn(Optional.of(testUser));

		Optional<Users> foundUser = repository.findById(userId);

		assertTrue(foundUser.isPresent());
		assertEquals(userId, foundUser.get().getId());
		verify(repository, times(1)).findById(userId);
	}

	@Test
	@DisplayName("Should find user by email")
	void testFindByEmail() {
		when(repository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));

		Optional<Users> foundUser = repository.findByEmail("john.doe@example.com");

		assertTrue(foundUser.isPresent());
		assertEquals("john.doe@example.com", foundUser.get().getEmail());
		verify(repository, times(1)).findByEmail("john.doe@example.com");
	}

	@Test
	@DisplayName("Should check if user exists by email")
	void testExistsByEmail() {
		when(repository.existsByEmail("john.doe@example.com")).thenReturn(true);

		boolean exists = repository.existsByEmail("john.doe@example.com");

		assertTrue(exists);
		verify(repository, times(1)).existsByEmail("john.doe@example.com");
	}

	@Test
	@DisplayName("Should delete user by id")
	void testDeleteById() {
		doNothing().when(repository).deleteById(userId);

		repository.deleteById(userId);

		verify(repository, times(1)).deleteById(userId);
	}
}
