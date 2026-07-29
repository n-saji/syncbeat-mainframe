package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackPresignedUrlResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.StreamUrlResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.Track;
import com.syncbeat.mainframe.syncbeatmainframe.repository.TrackRepository;
import com.syncbeat.mainframe.syncbeatmainframe.utils.CloudFrontUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing tracks and their associated operations.
 * Handles track creation, retrieval, updates, and streaming URL generation.
 */
@Slf4j
@Service
public class TrackService {

	private final TrackRepository trackRepository;
	private final S3Service s3Service;
	private final CloudFrontUtilities cloudFrontUtilities;

	public TrackService(TrackRepository trackRepository, S3Service s3Service, CloudFrontUtilities cloudFrontUtilities) {
		this.trackRepository = trackRepository;
		this.s3Service = s3Service;
		this.cloudFrontUtilities = cloudFrontUtilities;
	}

	/**
	 * Creates a new track with the provided information.
	 * The track is created without an S3 key initially; the S3 key is added
	 * after the file is uploaded to S3.
	 *
	 * @param trackRequestDto The track creation request containing title, artist, and duration
	 * @return The created track as a response DTO
	 */
	@Transactional
	public TrackResponseDto createTrack(TrackRequestDto trackRequestDto) {
		Track track = Track.builder()
				.title(trackRequestDto.getTitle())
				.artist(trackRequestDto.getArtist())
				.durationMs(trackRequestDto.getDurationMs())
				.build();

		Track savedTrack = trackRepository.save(track);
		log.info("Created track with ID: {} and title: {}", savedTrack.getId(), savedTrack.getTitle());
		return TrackResponseDto.fromEntity(savedTrack);
	}

	/**
	 * Generates a presigned URL for uploading a track file to S3.
	 * The URL is valid for the configured expiration time and points to
	 * the location: tracks/{trackId}/original.mp3
	 *
	 * @param trackId The UUID of the track
	 * @return Response containing the track ID, presigned URL, and upload path
	 */
	@Transactional(readOnly = true)
	public TrackPresignedUrlResponseDto getPresignedUploadUrl(UUID trackId) {
		// Verify track exists
		Track track = trackRepository.findById(trackId)
				.orElseThrow(() -> new IllegalArgumentException("Track not found: " + trackId));

		String presignedUrl = s3Service.generatePresignedUploadUrl(trackId);
		String uploadPath = s3Service.getTrackS3Key(trackId);

		log.info("Generated presigned upload URL for track: {}", trackId);
		return TrackPresignedUrlResponseDto.builder()
				.trackId(trackId)
				.presignedUrl(presignedUrl)
				.uploadPath(uploadPath)
				.build();
	}

	/**
	 * Updates the S3 key for a track after the file has been uploaded.
	 * This is typically called after the client uploads the file using the presigned URL.
	 *
	 * @param trackId The UUID of the track
	 * @param s3Key The S3 key where the file has been uploaded
	 * @return The updated track as a response DTO
	 */
	@Transactional
	public TrackResponseDto updateTrackS3Key(UUID trackId, String s3Key) {
		Track track = trackRepository.findById(trackId)
				.orElseThrow(() -> new IllegalArgumentException("Track not found: " + trackId));

		track.setS3Key(s3Key);
		Track updatedTrack = trackRepository.save(track);

		log.info("Updated S3 key for track {}: {}", trackId, s3Key);
		return TrackResponseDto.fromEntity(updatedTrack);
	}

	/**
	 * Retrieves a track by its ID.
	 *
	 * @param trackId The UUID of the track
	 * @return The track as a response DTO
	 */
	@Transactional(readOnly = true)
	public TrackResponseDto getTrackById(UUID trackId) {
		Track track = trackRepository.findById(trackId)
				.orElseThrow(() -> new IllegalArgumentException("Track not found: " + trackId));
		return TrackResponseDto.fromEntity(track);
	}

	/**
	 * Lists all available tracks in the catalog.
	 * This returns all tracks regardless of upload status.
	 *
	 * @return List of all tracks
	 */
	@Transactional(readOnly = true)
	public List<TrackResponseDto> getAllTracks() {
		return trackRepository.findAll()
				.stream()
				.map(TrackResponseDto::fromEntity)
				.collect(Collectors.toList());
	}

	/**
	 * Generates a signed CloudFront URL for streaming a track.
	 * The URL is valid for the configured CloudFront expiration time.
	 * This method should only be called for tracks that have a valid S3 key.
	 *
	 * @param trackId The UUID of the track to stream
	 * @return Response containing the track ID, signed streaming URL, and expiration timestamp
	 */
	@Transactional(readOnly = true)
	public StreamUrlResponseDto getStreamUrl(UUID trackId) {
		Track track = trackRepository.findById(trackId)
				.orElseThrow(() -> new IllegalArgumentException("Track not found: " + trackId));

		if (track.getS3Key() == null || track.getS3Key().isEmpty()) {
			throw new IllegalArgumentException("Track file not yet uploaded: " + trackId);
		}

		try {
			String streamUrl = cloudFrontUtilities.getSignedUrlWithCannedPolicy(track.getS3Key());
			long expiresAt = cloudFrontUtilities.getExpirationTimestamp();

			log.info("Generated signed streaming URL for track: {}", trackId);
			return StreamUrlResponseDto.builder()
					.trackId(trackId)
					.streamUrl(streamUrl)
					.expiresAt(expiresAt)
					.build();
		} catch (Exception e) {
			log.error("Error generating streaming URL for track: {}", trackId, e);
			throw new RuntimeException("Failed to generate streaming URL", e);
		}
	}

	/**
	 * Deletes a track by its ID.
	 *
	 * @param trackId The UUID of the track to delete
	 */
	@Transactional
	public void deleteTrack(UUID trackId) {
		if (!trackRepository.existsById(trackId)) {
			throw new IllegalArgumentException("Track not found: " + trackId);
		}
		trackRepository.deleteById(trackId);
		log.info("Deleted track: {}", trackId);
	}
}

