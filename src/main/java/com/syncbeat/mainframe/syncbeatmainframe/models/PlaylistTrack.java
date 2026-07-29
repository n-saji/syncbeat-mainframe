package com.syncbeat.mainframe.syncbeatmainframe.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "playlist_tracks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistTrack {
	@EmbeddedId
	private PlaylistTrackId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "playlist_id", insertable = false, updatable = false)
	private Playlist playlist;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "track_id", insertable = false, updatable = false)
	private Track track;

	@Embeddable
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class PlaylistTrackId implements Serializable {
		@Column(name = "playlist_id")
		private UUID playlistId;

		@Column(name = "track_id")
		private UUID trackId;
	}
}

