package com.syncbeat.mainframe.syncbeatmainframe.repository;

import com.syncbeat.mainframe.syncbeatmainframe.models.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {
	List<Playlist> findByUserId(UUID userId);
}

