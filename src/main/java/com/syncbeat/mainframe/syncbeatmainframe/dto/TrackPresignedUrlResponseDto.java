package com.syncbeat.mainframe.syncbeatmainframe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackPresignedUrlResponseDto {
	@JsonProperty("track_id")
	private UUID trackId;

	@JsonProperty("presigned_url")
	private String presignedUrl;

	@JsonProperty("upload_path")
	private String uploadPath;
}

