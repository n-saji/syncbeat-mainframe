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


@Slf4j
@RestController
@RequestMapping("/api/v1/tracks")
public class TrackController {

	private final TrackService trackService;

	public TrackController(TrackService trackService) {
		this.trackService = trackService;
	}


	@PostMapping
	public ResponseEntity<TrackResponseDto> createTrack(@Valid @RequestBody TrackRequestDto trackRequestDto) {
		if (!currentUser().getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		log.info("Creating new track: {}", trackRequestDto.getTitle());
		TrackResponseDto createdTrack = trackService.createTrack(trackRequestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdTrack);
	}


	@GetMapping
	public ResponseEntity<List<TrackResponseDto>> getAllTracks() {
		log.info("Fetching all tracks");
		List<TrackResponseDto> tracks = trackService.getAllTracks();
		return ResponseEntity.ok(tracks);
	}


	@GetMapping("/trending")
	public ResponseEntity<List<TrackResponseDto>> getTrendingTracks(
			@RequestParam(defaultValue = "10") int limit) {
		log.info("Fetching top {} trending tracks", limit);
		List<TrackResponseDto> tracks = trackService.getTrendingTracks(limit);
		return ResponseEntity.ok(tracks);
	}


	@GetMapping("/{id}")
	public ResponseEntity<TrackResponseDto> getTrackById(@PathVariable UUID id) {
		log.info("Fetching track with ID: {}", id);
		TrackResponseDto track = trackService.getTrackById(id);
		return ResponseEntity.ok(track);
	}

	@GetMapping("/{id}/presigned-url")
	public ResponseEntity<TrackPresignedUrlResponseDto> getPresignedUploadUrl(@PathVariable UUID id) {
		if (!currentUser().getAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		log.info("Requesting presigned URL for track: {}", id);
		TrackPresignedUrlResponseDto presignedUrlResponse = trackService.getPresignedUploadUrl(id);
		return ResponseEntity.ok(presignedUrlResponse);
	}


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


	@GetMapping("/{id}/stream-url")
	public ResponseEntity<StreamUrlResponseDto> getStreamUrl(@PathVariable UUID id) {
		log.info("Requesting stream URL for track: {}", id);
		StreamUrlResponseDto streamUrl = trackService.getStreamUrl(id);
		return ResponseEntity.ok(streamUrl);
	}


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


