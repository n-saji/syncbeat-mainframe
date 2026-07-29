package com.syncbeat.mainframe.syncbeatmainframe.repository;

import com.syncbeat.mainframe.syncbeatmainframe.models.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, PlaylistTrack.PlaylistTrackId> {
	List<PlaylistTrack> findByIdPlaylistId(UUID playlistId);

	@Query("SELECT pt FROM PlaylistTrack pt WHERE pt.id.playlistId = :playlistId ORDER BY pt.id.trackId")
	List<PlaylistTrack> findByIdPlaylistIdOrderByTrackId(UUID playlistId);

	void deleteByIdPlaylistId(UUID playlistId);

	void deleteByIdPlaylistIdAndIdTrackId(UUID playlistId, UUID trackId);
}

