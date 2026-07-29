package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaylistDetailResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaylistRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaylistResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import com.syncbeat.mainframe.syncbeatmainframe.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/playlists")
public class PlaylistController {

	private final PlaylistService playlistService;

	public PlaylistController(PlaylistService playlistService) {
		this.playlistService = playlistService;
	}


	@PostMapping
	public ResponseEntity<PlaylistResponseDto> createPlaylist(
			@Valid @RequestBody PlaylistRequestDto playlistRequestDto,
			Authentication authentication) {
		UUID userId = currentUser(authentication).getId();
		log.info("Creating playlist for user: {} with name: {}", userId, playlistRequestDto.getName());
		PlaylistResponseDto createdPlaylist = playlistService.createPlaylist(userId, playlistRequestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdPlaylist);
	}

	@GetMapping
	public ResponseEntity<List<PlaylistResponseDto>> getUserPlaylists(Authentication authentication) {
		UUID userId = currentUser(authentication).getId();
		log.info("Fetching playlists for user: {}", userId);
		List<PlaylistResponseDto> playlists = playlistService.getUserPlaylists(userId);
		return ResponseEntity.ok(playlists);
	}


	@GetMapping("/{id}")
	public ResponseEntity<PlaylistDetailResponseDto> getPlaylistWithTracks(
			@PathVariable UUID id,
			Authentication authentication) {
		User user = currentUser(authentication);
		UUID userId = user.getId();
		log.info("Fetching playlist {} for user: {}", id, userId);

		// Verify ownership (or allow admin to access any playlist)
		PlaylistDetailResponseDto playlistDetail = playlistService.getPlaylistWithTracks(id);
		if (!playlistDetail.getUserId().equals(userId) && !user.getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		return ResponseEntity.ok(playlistDetail);
	}


	@PostMapping("/{id}/tracks")
	public ResponseEntity<Void> addTracksToPlaylist(
			@PathVariable UUID id,
			@Valid @RequestBody PlaylistRequestDto playlistRequestDto,
			Authentication authentication) {
		User user = currentUser(authentication);
		UUID userId = user.getId();
		log.info("Adding tracks to playlist {} for user: {}", id, userId);

		// Verify ownership
		PlaylistResponseDto playlist = playlistService.getPlaylistById(id);
		if (!playlist.getUserId().equals(userId) && !user.getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		if (playlistRequestDto.getTrackIds() != null && !playlistRequestDto.getTrackIds().isEmpty()) {
			playlistService.addTracksToPlaylist(id, playlistRequestDto.getTrackIds());
		}

		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{playlistId}/tracks/{trackId}")
	public ResponseEntity<Void> removeTrackFromPlaylist(
			@PathVariable UUID playlistId,
			@PathVariable UUID trackId,
			Authentication authentication) {
		User user = currentUser(authentication);
		UUID userId = user.getId();
		log.info("Removing track {} from playlist {} for user: {}", trackId, playlistId, userId);

		// Verify ownership
		PlaylistResponseDto playlist = playlistService.getPlaylistById(playlistId);
		if (!playlist.getUserId().equals(userId) && !user.getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		playlistService.removeTrackFromPlaylist(playlistId, trackId);
		return ResponseEntity.noContent().build();
	}


	@PutMapping("/{id}")
	public ResponseEntity<PlaylistResponseDto> updatePlaylist(
			@PathVariable UUID id,
			@Valid @RequestBody PlaylistRequestDto playlistRequestDto,
			Authentication authentication) {
		User user = currentUser(authentication);
		UUID userId = user.getId();
		log.info("Updating playlist {} for user: {}", id, userId);

		// Verify ownership
		PlaylistResponseDto playlist = playlistService.getPlaylistById(id);
		if (!playlist.getUserId().equals(userId) && !user.getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		PlaylistResponseDto updatedPlaylist = playlistService.updatePlaylistName(id, playlistRequestDto.getName());
		return ResponseEntity.ok(updatedPlaylist);
	}


	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletePlaylist(
			@PathVariable UUID id,
			Authentication authentication) {
		User user = currentUser(authentication);
		UUID userId = user.getId();
		log.info("Deleting playlist {} for user: {}", id, userId);

		// Verify ownership
		PlaylistResponseDto playlist = playlistService.getPlaylistById(id);
		if (!playlist.getUserId().equals(userId) && !user.getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		playlistService.deletePlaylist(id);
		return ResponseEntity.noContent().build();
	}


	private User currentUser(Authentication authentication) {
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new IllegalArgumentException("User not authenticated");
		}
		return (User) authentication.getPrincipal();
	}
}


