package com.syncbeat.mainframe.syncbeatmainframe.controller;

import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackPresignedUrlResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackUpdateS3KeyRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.StreamUrlResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import com.syncbeat.mainframe.syncbeatmainframe.service.TrackService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Track management.
 * Handles track creation, listing, and streaming operations.
 *
 * Base Path: /api/v1/tracks
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tracks")
public class TrackController {

	private final TrackService trackService;

	public TrackController(TrackService trackService) {
		this.trackService = trackService;
	}

	/**
	 * Creates a new track.
	 * Only accessible to admin users.
	 *
	 * POST /api/v1/tracks
	 *
	 * @param trackRequestDto The track details (title, artist, duration_ms)
	 * @return ResponseEntity with the created track and 201 status
	 */
	@PostMapping
	public ResponseEntity<TrackResponseDto> createTrack(@Valid @RequestBody TrackRequestDto trackRequestDto) {
		if (!currentUser().getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		log.info("Creating new track: {}", trackRequestDto.getTitle());
		TrackResponseDto createdTrack = trackService.createTrack(trackRequestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdTrack);
	}

	/**
	 * Lists all tracks in the catalog.
	 * Accessible to all authenticated users.
	 *
	 * GET /api/v1/tracks
	 *
	 * @return ResponseEntity with list of all tracks
	 */
	@GetMapping
	public ResponseEntity<List<TrackResponseDto>> getAllTracks() {
		log.info("Fetching all tracks");
		List<TrackResponseDto> tracks = trackService.getAllTracks();
		return ResponseEntity.ok(tracks);
	}

	/**
	 * Gets a specific track by ID.
	 *
	 * GET /api/v1/tracks/{id}
	 *
	 * @param id The track UUID
	 * @return ResponseEntity with the track details
	 */
	@GetMapping("/{id}")
	public ResponseEntity<TrackResponseDto> getTrackById(@PathVariable UUID id) {
		log.info("Fetching track with ID: {}", id);
		TrackResponseDto track = trackService.getTrackById(id);
		return ResponseEntity.ok(track);
	}

	/**
	 * Generates a presigned URL for uploading a track file to S3.
	 * Only accessible to admin users.
	 * The URL is valid for a limited time and points to: tracks/{trackId}/original.mp3
	 *
	 * GET /api/v1/tracks/{id}/presigned-url
	 *
	 * @param id The track UUID
	 * @return ResponseEntity with presigned URL and upload path
	 */
	@GetMapping("/{id}/presigned-url")
	public ResponseEntity<TrackPresignedUrlResponseDto> getPresignedUploadUrl(@PathVariable UUID id) {
		if (!currentUser().getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		log.info("Requesting presigned URL for track: {}", id);
		TrackPresignedUrlResponseDto presignedUrlResponse = trackService.getPresignedUploadUrl(id);
		return ResponseEntity.ok(presignedUrlResponse);
	}

	/**
	 * Updates the S3 key for a track after file upload.
	 * Only accessible to admin users.
	 * This should be called after the client uploads the file using the presigned URL.
	 *
	 * PATCH /api/v1/tracks/{id}/s3-key
	 *
	 * @param id The track UUID
	 * @param updateRequest The S3 key update request
	 * @return ResponseEntity with the updated track
	 */
	@PatchMapping("/{id}/s3-key")
	public ResponseEntity<TrackResponseDto> updateTrackS3Key(
			@PathVariable UUID id,
			@Valid @RequestBody TrackUpdateS3KeyRequestDto updateRequest) {
		if (!currentUser().getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		log.info("Updating S3 key for track: {}", id);
		TrackResponseDto updatedTrack = trackService.updateTrackS3Key(id, updateRequest.getS3Key());
		return ResponseEntity.ok(updatedTrack);
	}

	/**
	 * Generates a signed CloudFront URL for streaming a track.
	 * The URL is valid for a limited time and should be used in the <audio> tag on the client.
	 * Only accessible to authenticated users.
	 *
	 * GET /api/v1/tracks/{id}/stream-url
	 *
	 * @param id The track UUID
	 * @return ResponseEntity with signed streaming URL and expiration timestamp
	 */
	@GetMapping("/{id}/stream-url")
	public ResponseEntity<StreamUrlResponseDto> getStreamUrl(@PathVariable UUID id) {
		log.info("Requesting stream URL for track: {}", id);
		StreamUrlResponseDto streamUrl = trackService.getStreamUrl(id);
		return ResponseEntity.ok(streamUrl);
	}

	/**
	 * Deletes a track.
	 * Only accessible to admin users.
	 *
	 * DELETE /api/v1/tracks/{id}
	 *
	 * @param id The track UUID
	 * @return ResponseEntity with 204 No Content status
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTrack(@PathVariable UUID id) {
		if (!currentUser().getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		log.info("Deleting track: {}", id);
		trackService.deleteTrack(id);
		return ResponseEntity.noContent().build();
	}

	private User currentUser() {
		return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}
}


