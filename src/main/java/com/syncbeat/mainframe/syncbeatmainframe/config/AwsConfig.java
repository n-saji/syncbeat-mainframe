package com.syncbeat.mainframe.syncbeatmainframe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;

/**
 * Configuration class for AWS services.
 * Sets up S3 client and presigner beans with support for LocalStack development.
 */
@Slf4j
@Configuration
public class AwsConfig {

	@Value("${spring.cloud.aws.credentials.access-key:test}")
	private String awsAccessKey;

	@Value("${spring.cloud.aws.credentials.secret-key:test}")
	private String awsSecretKey;

	@Value("${spring.cloud.aws.region.static:us-east-1}")
	private String awsRegion;

	@Value("${spring.cloud.aws.endpoint:}")
	private String awsEndpoint;

	/**
	 * Creates an S3Client bean configured with AWS credentials and region.
	 * If an endpoint is configured (e.g., for LocalStack), it will override the default AWS endpoint.
	 *
	 * @return Configured S3Client
	 */
	@Bean
	public S3Client s3Client() {
		log.info("Configuring S3 client for region: {}", awsRegion);

		var builder = S3Client.builder()
				.region(Region.of(awsRegion))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
				));

		if (awsEndpoint != null && !awsEndpoint.isEmpty()) {
			log.info("Using custom S3 endpoint: {}", awsEndpoint);
			builder.endpointOverride(URI.create(awsEndpoint));
		}

		return builder.build();
	}

	/**
	 * Creates an S3Presigner bean for generating presigned URLs.
	 * Uses the same credentials and endpoint configuration as the S3Client.
	 *
	 * @return Configured S3Presigner
	 */
	@Bean
	public S3Presigner s3Presigner() {
		log.info("Configuring S3 Presigner");

		var builder = S3Presigner.builder()
				.region(Region.of(awsRegion))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
				));

		if (awsEndpoint != null && !awsEndpoint.isEmpty()) {
			log.info("Using custom S3 presigner endpoint: {}", awsEndpoint);
			builder.endpointOverride(URI.create(awsEndpoint));
		}

		return builder.build();
	}
}



