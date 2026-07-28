package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.RoomResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.Room;
import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import com.syncbeat.mainframe.syncbeatmainframe.repository.RoomRepository;
import com.syncbeat.mainframe.syncbeatmainframe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RoomService {
	private final RoomRepository roomRepository;
	private final UserRepository userRepository;

	public RoomResponseDto createRoom(
	                                  RoomRequestDto roomRequestDto) {
		User user = (User) Objects.requireNonNull(SecurityContextHolder
						.getContext()
						.getAuthentication())
				.getPrincipal();

		return RoomResponseDto.fromEntity(roomRepository.save(Room.builder()
				.name(roomRequestDto.getName())
				.active(roomRequestDto.isActive())
				.isPublic(roomRequestDto.isPublic())
				.createdBy(user)
				.build()));
	}
}
