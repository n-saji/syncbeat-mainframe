package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackPresignedUrlResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.StreamUrlResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.Track;
import com.syncbeat.mainframe.syncbeatmainframe.repository.TrackRepository;
import com.syncbeat.mainframe.syncbeatmainframe.utils.CloudFrontUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


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


	@Transactional(readOnly = true)
	public TrackPresignedUrlResponseDto getPresignedUploadUrl(UUID trackId) {
		// Verify track exists
		Track track = trackRepository.findById(trackId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found: " + trackId));

		String presignedUrl = s3Service.generatePresignedUploadUrl(trackId);
		String s3Key = s3Service.getTrackS3Key(trackId);

		log.info("Generated presigned upload URL for track: {}", trackId);
		return TrackPresignedUrlResponseDto.builder()
				.trackId(trackId)
				.presignedUrl(presignedUrl)
				.s3Key(s3Key)
				.build();
	}


	@Transactional
	public TrackResponseDto updateTrackS3Key(UUID trackId, String s3Key) {
		Track track = trackRepository.findById(trackId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found: " + trackId));

		track.setS3Key(s3Key);
		Track updatedTrack = trackRepository.save(track);

		log.info("Updated S3 key for track {}: {}", trackId, s3Key);
		return TrackResponseDto.fromEntity(updatedTrack);
	}


	@Transactional(readOnly = true)
	public TrackResponseDto getTrackById(UUID trackId) {
		Track track = trackRepository.findById(trackId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found: " + trackId));
		return TrackResponseDto.fromEntity(track);
	}


	@Transactional(readOnly = true)
	public List<TrackResponseDto> getAllTracks() {
		return trackRepository.findAll()
				.stream()
				.map(TrackResponseDto::fromEntity)
				.collect(Collectors.toList());
	}


	private static final int MAX_TRENDING_LIMIT = 100;

	// play_count is only as fresh as syncbeat-analytics's last flush (every few
	// minutes, see that service's README) - this reads the durable Postgres total,
	// not the live Redis counters.
	@Transactional(readOnly = true)
	public List<TrackResponseDto> getTrendingTracks(int limit) {
		int clampedLimit = Math.min(Math.max(limit, 1), MAX_TRENDING_LIMIT);
		return trackRepository.findAllByOrderByPlayCountDesc(PageRequest.of(0, clampedLimit))
				.stream()
				.map(TrackResponseDto::fromEntity)
				.collect(Collectors.toList());
	}


	@Transactional(readOnly = true)
	public StreamUrlResponseDto getStreamUrl(UUID trackId) {
		Track track = trackRepository.findById(trackId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found: " + trackId));

		if (track.getS3Key() == null || track.getS3Key().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Track file not yet uploaded: " + trackId);
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


	@Transactional
	public void deleteTrack(UUID trackId) {
		if (!trackRepository.existsById(trackId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found: " + trackId);
		}
		trackRepository.deleteById(trackId);
		log.info("Deleted track: {}", trackId);
	}
}

