package com.syncbeat.mainframe.syncbeatmainframe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.syncbeat.mainframe.syncbeatmainframe.dto.PlaybackEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaybackEventPublisher {
	private final SnsClient snsClient;

	// Owned locally rather than injected: this Spring Boot version's auto-configured
	// Jackson bean is Jackson 3 (tools.jackson.databind.json.JsonMapper), a separate
	// API from com.fasterxml.jackson.databind.ObjectMapper (Jackson 2, the version
	// already on the classpath via jjwt-jackson and used by @JsonProperty elsewhere).
	// JavaTimeModule registered defensively - see RedisService, which owns the same kind
	// of mapper and hit exactly this gap the moment it needed to serialize an Instant.
	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Value("${aws.sns.room-events-topic-arn}")
	private String topicArn;

	public void publish(PlaybackEventDto event) {
		String message;
		try {
			message = objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize playback event for room {}", event.getRoomId(), e);
			return;
		}

		String dedupId = event.getRoomId() + ":" + event.getTimestamp() + ":" + event.getEventType();
		try {
			snsClient.publish(PublishRequest.builder()
					.topicArn(topicArn)
					.message(message)
					.messageGroupId(event.getRoomId())
					.messageDeduplicationId(dedupId)
					.build());
		} catch (Exception e) {
			log.error("Failed to publish playback event for room {} to SNS", event.getRoomId(), e);
		}
	}
}
