package com.syncbeat.mainframe.syncbeatmainframe.repository;

import com.syncbeat.mainframe.syncbeatmainframe.models.Room;
import com.syncbeat.mainframe.syncbeatmainframe.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
	Collection<Room> findByCreatedBy(User createdBy);
	Collection<Room> findByCreatedByAndActive(User user, boolean b);

	// Excludes the caller's own rooms - those already surface under "my rooms", so this is
	// purely the "browse what else is live" listing.
	@Query("SELECT r FROM Room r WHERE r.isPublic = true AND r.active = true AND r.createdBy <> :user ORDER BY r.updatedAt DESC")
	Collection<Room> findDiscoverablePublicRooms(@Param("user") User user);
}
