package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaylistDetailResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaylistRequestDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaylistResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.dto.TrackResponseDto;
import com.syncbeat.mainframe.syncbeatmainframe.models.Playlist;
import com.syncbeat.mainframe.syncbeatmainframe.models.PlaylistTrack;
import com.syncbeat.mainframe.syncbeatmainframe.repository.PlaylistRepository;
import com.syncbeat.mainframe.syncbeatmainframe.repository.PlaylistTrackRepository;
import com.syncbeat.mainframe.syncbeatmainframe.repository.TrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
public class PlaylistService {

	private final PlaylistRepository playlistRepository;
	private final PlaylistTrackRepository playlistTrackRepository;
	private final TrackRepository trackRepository;

	public PlaylistService(PlaylistRepository playlistRepository,
						  PlaylistTrackRepository playlistTrackRepository,
						  TrackRepository trackRepository) {
		this.playlistRepository = playlistRepository;
		this.playlistTrackRepository = playlistTrackRepository;
		this.trackRepository = trackRepository;
	}

	@Transactional
	public PlaylistResponseDto createPlaylist(UUID userId, PlaylistRequestDto playlistRequestDto) {
		Playlist playlist = Playlist.builder()
				.name(playlistRequestDto.getName())
				.userId(userId)
				.build();

		Playlist savedPlaylist = playlistRepository.save(playlist);
		log.info("Created playlist with ID: {} for user: {}", savedPlaylist.getId(), userId);

		// Add tracks if provided
		if (playlistRequestDto.getTrackIds() != null && !playlistRequestDto.getTrackIds().isEmpty()) {
			addTracksToPlaylist(savedPlaylist.getId(), playlistRequestDto.getTrackIds());
		}

		return PlaylistResponseDto.fromEntity(savedPlaylist);
	}

	@Transactional
	public void addTracksToPlaylist(UUID playlistId, List<String> trackIds) {
		if (!playlistRepository.existsById(playlistId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found: " + playlistId);
		}

		for (String trackIdStr : trackIds) {
			try {
				UUID trackId = UUID.fromString(trackIdStr);

				// Verify track exists
				if (!trackRepository.existsById(trackId)) {
					log.warn("Track not found: {}", trackId);
					continue;
				}

				// Create playlist track association
				PlaylistTrack.PlaylistTrackId id = PlaylistTrack.PlaylistTrackId.builder()
						.playlistId(playlistId)
						.trackId(trackId)
						.build();

				PlaylistTrack playlistTrack = PlaylistTrack.builder()
						.id(id)
						.build();

				playlistTrackRepository.save(playlistTrack);
				log.debug("Added track {} to playlist {}", trackId, playlistId);
			} catch (IllegalArgumentException e) {
				log.warn("Invalid track ID format: {}", trackIdStr);
			}
		}
	}

	@Transactional
	public void removeTrackFromPlaylist(UUID playlistId, UUID trackId) {
		if (!playlistRepository.existsById(playlistId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found: " + playlistId);
		}

		playlistTrackRepository.deleteByIdPlaylistIdAndIdTrackId(playlistId, trackId);
		log.info("Removed track {} from playlist {}", trackId, playlistId);
	}


	@Transactional(readOnly = true)
	public PlaylistDetailResponseDto getPlaylistWithTracks(UUID playlistId) {
		Playlist playlist = playlistRepository.findById(playlistId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found: " + playlistId));

		List<PlaylistTrack> playlistTracks = playlistTrackRepository.findByIdPlaylistIdOrderByTrackId(playlistId);
		List<TrackResponseDto> tracks = playlistTracks.stream()
				.map(PlaylistTrack::getTrack)
				.map(TrackResponseDto::fromEntity)
				.collect(Collectors.toList());

		log.info("Retrieved playlist {} with {} tracks", playlistId, tracks.size());
		return PlaylistDetailResponseDto.builder()
				.id(playlist.getId())
				.name(playlist.getName())
				.userId(playlist.getUserId())
				.tracks(tracks)
				.createdAt(playlist.getCreatedAt())
				.updatedAt(playlist.getUpdatedAt())
				.build();
	}

	@Transactional(readOnly = true)
	public PlaylistResponseDto getPlaylistById(UUID playlistId) {
		Playlist playlist = playlistRepository.findById(playlistId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found: " + playlistId));
		return PlaylistResponseDto.fromEntity(playlist);
	}


	@Transactional(readOnly = true)
	public List<PlaylistResponseDto> getUserPlaylists(UUID userId) {
		return playlistRepository.findByUserId(userId)
				.stream()
				.map(PlaylistResponseDto::fromEntity)
				.collect(Collectors.toList());
	}


	@Transactional
	public PlaylistResponseDto updatePlaylistName(UUID playlistId, String newName) {
		Playlist playlist = playlistRepository.findById(playlistId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found: " + playlistId));

		playlist.setName(newName);
		Playlist updatedPlaylist = playlistRepository.save(playlist);

		log.info("Updated playlist {} name to: {}", playlistId, newName);
		return PlaylistResponseDto.fromEntity(updatedPlaylist);
	}

	@Transactional
	public void deletePlaylist(UUID playlistId) {
		if (!playlistRepository.existsById(playlistId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found: " + playlistId);
		}

		// Delete all associated playlist tracks
		playlistTrackRepository.deleteByIdPlaylistId(playlistId);

		// Delete the playlist itself
		playlistRepository.deleteById(playlistId);
		log.info("Deleted playlist: {}", playlistId);
	}


	@Transactional(readOnly = true)
	public int getPlaylistTrackCount(UUID playlistId) {
		return playlistTrackRepository.findByIdPlaylistId(playlistId).size();
	}
}


