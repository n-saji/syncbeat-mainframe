package com.syncbeat.mainframe.syncbeatmainframe.repository;

import com.syncbeat.mainframe.syncbeatmainframe.models.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TrackRepository extends JpaRepository<Track, UUID> {
}

