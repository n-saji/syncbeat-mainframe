package com.syncbeat.mainframe.syncbeatmainframe.dto;

/**
 * Mirrors syncbeat_sync.models.EventType — values must stay identical (serialized
 * as-is via enum name) since syncbeat-sync deserializes this exact string.
 */
public enum PlaybackEventType {
	PLAY,
	PAUSE,
	SEEK,
	SKIP,
	SYNC_PULSE,
	HOST_CHANGED
}
