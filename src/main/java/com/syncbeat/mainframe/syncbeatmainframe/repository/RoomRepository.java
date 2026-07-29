package com.syncbeat.mainframe.syncbeatmainframe.repository;

import com.syncbeat.mainframe.syncbeatmainframe.models.Room;
import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
	Collection<Room> findByCreatedBy(User createdBy);
	Collection<Room> findByCreatedByAndActive(User user, boolean b);
}
