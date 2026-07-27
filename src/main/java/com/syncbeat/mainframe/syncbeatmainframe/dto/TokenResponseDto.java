package com.syncbeat.mainframe.syncbeatmainframe.dto;

public record TokenResponseDto(
		String accessToken,
		String refreshToken
) {
}
