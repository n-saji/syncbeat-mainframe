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

/**
 * REST Controller for Playlist management.
 * Handles playlist creation, listing, and track management within playlists.
 *
 * Base Path: /api/v1/playlists
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/playlists")
public class PlaylistController {

	private final PlaylistService playlistService;

	public PlaylistController(PlaylistService playlistService) {
		this.playlistService = playlistService;
	}

	/**
	 * Creates a new playlist for the authenticated user.
	 * Users can only create playlists for themselves.
	 *
	 * POST /api/v1/playlists
	 *
	 * @param playlistRequestDto The playlist details (name, optional track_ids)
	 * @param authentication The authentication object containing user details
	 * @return ResponseEntity with the created playlist and 201 status
	 */
	@PostMapping
	public ResponseEntity<PlaylistResponseDto> createPlaylist(
			@Valid @RequestBody PlaylistRequestDto playlistRequestDto,
			Authentication authentication) {
		UUID userId = currentUser(authentication).getId();
		log.info("Creating playlist for user: {} with name: {}", userId, playlistRequestDto.getName());
		PlaylistResponseDto createdPlaylist = playlistService.createPlaylist(userId, playlistRequestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdPlaylist);
	}

	/**
	 * Lists all playlists for the authenticated user.
	 *
	 * GET /api/v1/playlists
	 *
	 * @param authentication The authentication object containing user details
	 * @return ResponseEntity with list of user's playlists
	 */
	@GetMapping
	public ResponseEntity<List<PlaylistResponseDto>> getUserPlaylists(Authentication authentication) {
		UUID userId = currentUser(authentication).getId();
		log.info("Fetching playlists for user: {}", userId);
		List<PlaylistResponseDto> playlists = playlistService.getUserPlaylists(userId);
		return ResponseEntity.ok(playlists);
	}

	/**
	 * Gets a specific playlist by ID with all its tracks.
	 * Only the owner of the playlist can view it.
	 *
	 * GET /api/v1/playlists/{id}
	 *
	 * @param id The playlist UUID
	 * @param authentication The authentication object containing user details
	 * @return ResponseEntity with the playlist and its tracks
	 */
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

	/**
	 * Adds tracks to an existing playlist.
	 * Only the owner of the playlist can modify it.
	 *
	 * POST /api/v1/playlists/{id}/tracks
	 *
	 * @param id The playlist UUID
	 * @param playlistRequestDto Request containing track_ids to add
	 * @param authentication The authentication object containing user details
	 * @return ResponseEntity with 204 No Content status
	 */
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

	/**
	 * Removes a track from a playlist.
	 * Only the owner of the playlist can modify it.
	 *
	 * DELETE /api/v1/playlists/{playlistId}/tracks/{trackId}
	 *
	 * @param playlistId The playlist UUID
	 * @param trackId The track UUID to remove
	 * @param authentication The authentication object containing user details
	 * @return ResponseEntity with 204 No Content status
	 */
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

	/**
	 * Updates a playlist's name.
	 * Only the owner of the playlist can modify it.
	 *
	 * PUT /api/v1/playlists/{id}
	 *
	 * @param id The playlist UUID
	 * @param playlistRequestDto Request containing the new name
	 * @param authentication The authentication object containing user details
	 * @return ResponseEntity with the updated playlist
	 */
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

	/**
	 * Deletes a playlist.
	 * Only the owner of the playlist can delete it.
	 *
	 * DELETE /api/v1/playlists/{id}
	 *
	 * @param id The playlist UUID
	 * @param authentication The authentication object containing user details
	 * @return ResponseEntity with 204 No Content status
	 */
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

	/**
	 * Helper method to extract user ID from the authentication principal.
	 * Expects the principal to be a string UUID.
	 *
	 * @param authentication The authentication object
	 * @return The user UUID
	 */
	private User currentUser(Authentication authentication) {
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new IllegalArgumentException("User not authenticated");
		}
		return (User) authentication.getPrincipal();
	}
}


