package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncbeat.mainframe.syncbeatmainframe.dto.UserRequestDto;

import com.syncbeat.mainframe.syncbeatmainframe.dto.UserResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.service.userService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class userControllerTest {

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	@Mock
	private userService userService;

	@InjectMocks
	private userController userController;

	private UUID userId;
	private UserResponseDto responseDto;
	private UserRequestDto requestDto;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
		objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();


		userId = UUID.randomUUID();

		responseDto = UserResponseDto.builder()
				.id(userId)
				.f_name("Jane")
				.l_name("Doe")
				.email("jane.doe@example.com")
				.is_admin(false)
				.is_active(true)
				.created_at(LocalDateTime.now())
				.updated_at(LocalDateTime.now())
				.build();

		requestDto = UserRequestDto.builder()
				.f_name("Jane")
				.l_name("Doe")
				.email("jane.doe@example.com")
				.password("secret123")
				.is_admin(false)
				.is_active(true)
				.build();
	}

	@Test
	@DisplayName("POST /api/users - Should create user and return 201 CREATED")
	void testCreateUser() throws Exception {
		when(userService.createUser(any(UserRequestDto.class))).thenReturn(responseDto);

		mockMvc.perform(post("/api/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requestDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(userId.toString()))
				.andExpect(jsonPath("$.f_name").value("Jane"))
				.andExpect(jsonPath("$.email").value("jane.doe@example.com"));
	}

	@Test
	@DisplayName("GET /api/users - Should return list of users and 200 OK")
	void testGetAllUsers() throws Exception {
		when(userService.getAllUsers()).thenReturn(List.of(responseDto));

		mockMvc.perform(get("/api/users"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(userId.toString()))
				.andExpect(jsonPath("$[0].f_name").value("Jane"));
	}

	@Test
	@DisplayName("GET /api/users/{id} - Should return user by id and 200 OK")
	void testGetUserById() throws Exception {
		when(userService.getUserById(userId)).thenReturn(responseDto);

		mockMvc.perform(get("/api/users/{id}", userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userId.toString()))
				.andExpect(jsonPath("$.f_name").value("Jane"));
	}

	@Test
	@DisplayName("PUT /api/users/{id} - Should update user and return 200 OK")
	void testUpdateUser() throws Exception {
		when(userService.updateUser(eq(userId), any(UserRequestDto.class))).thenReturn(responseDto);

		mockMvc.perform(put("/api/users/{id}", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requestDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userId.toString()))
				.andExpect(jsonPath("$.f_name").value("Jane"));
	}

	@Test
	@DisplayName("DELETE /api/users/{id} - Should delete user and return 204 NO CONTENT")
	void testDeleteUser() throws Exception {
		doNothing().when(userService).deleteUser(userId);

		mockMvc.perform(delete("/api/users/{id}", userId))
				.andExpect(status().isNoContent());
	}
}
