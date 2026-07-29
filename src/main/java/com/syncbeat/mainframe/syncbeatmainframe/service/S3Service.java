package com.syncbeat.mainframe.syncbeatmainframe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;


@Slf4j
@Service
public class S3Service {

	private final S3Presigner s3Presigner;

	@Value("${aws.s3.bucket-name:syncbeat-tracks}")
	private String bucketName;

	@Value("${aws.s3.presigned-url-expiration:900}")
	private int presignedUrlExpirationSeconds;

	public S3Service(S3Presigner s3Presigner) {
		this.s3Presigner = s3Presigner;
	}

	public String generatePresignedUploadUrl(UUID trackId) {
		String s3Key = String.format("tracks/%s/original.mp3", trackId);

		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(s3Key)
					.contentType("audio/mpeg")
					.build();

			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
					.signatureDuration(Duration.ofSeconds(presignedUrlExpirationSeconds))
					.putObjectRequest(putObjectRequest)
					.build();

			String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
			log.info("Generated presigned upload URL for track: {}", trackId);
			return presignedUrl;
		} catch (Exception e) {
			log.error("Error generating presigned URL for track: {}", trackId, e);
			throw new RuntimeException("Failed to generate presigned URL", e);
		}
	}


	public String generatePresignedDownloadUrl(String s3Key) {
		try {
			GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
					.signatureDuration(Duration.ofSeconds(presignedUrlExpirationSeconds))
					.getObjectRequest(req -> req.bucket(bucketName).key(s3Key))
					.build();

			String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();
			log.info("Generated presigned download URL for key: {}", s3Key);
			return presignedUrl;
		} catch (Exception e) {
			log.error("Error generating presigned download URL for key: {}", s3Key, e);
			throw new RuntimeException("Failed to generate presigned download URL", e);
		}
	}

	public String getTrackS3Key(UUID trackId) {
		return String.format("tracks/%s/original.mp3", trackId);
	}
}


