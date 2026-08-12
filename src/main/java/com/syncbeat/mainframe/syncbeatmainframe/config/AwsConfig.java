package com.syncbeat.mainframe.syncbeatmainframe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;

@Slf4j
@Configuration
public class AwsConfig {

	@Value("${spring.cloud.aws.credentials.access-key:}")
	private String awsAccessKey;

	@Value("${spring.cloud.aws.credentials.secret-key:}")
	private String awsSecretKey;

	@Value("${spring.cloud.aws.region.static:us-east-1}")
	private String awsRegion;

	@Value("${spring.cloud.aws.endpoint:}")
	private String awsEndpoint;

	// LocalStack dev supplies explicit static keys via application-local.properties.
	// Real AWS leaves these unset, so this falls back to DefaultCredentialsProvider
	// (EC2 instance role / env vars / etc.) instead of ever using a literal "test" key.
	private AwsCredentialsProvider credentialsProvider() {
		if (awsAccessKey != null && !awsAccessKey.isEmpty() && awsSecretKey != null && !awsSecretKey.isEmpty()) {
			return StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKey, awsSecretKey));
		}
		return DefaultCredentialsProvider.builder().build();
	}

	@Bean
	public S3Client s3Client() {
		log.info("Configuring S3 client for region: {}", awsRegion);

		var builder = S3Client.builder()
				.region(Region.of(awsRegion))
				.credentialsProvider(credentialsProvider());

		if (awsEndpoint != null && !awsEndpoint.isEmpty()) {
			log.info("Using custom S3 endpoint: {}", awsEndpoint);
			builder.endpointOverride(URI.create(awsEndpoint))
					// Virtual-hosted-style (bucket-as-subdomain) addressing isn't reliably
					// resolved against LocalStack's *.localhost host — force path-style
					// (bucket in the URL path) whenever we're pointed at a custom endpoint.
					// Real AWS (no endpoint override) keeps the SDK default.
					.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
		}

		return builder.build();
	}

	@Bean
	public S3Presigner s3Presigner() {
		log.info("Configuring S3 Presigner");

		var builder = S3Presigner.builder()
				.region(Region.of(awsRegion))
				.credentialsProvider(credentialsProvider());

		if (awsEndpoint != null && !awsEndpoint.isEmpty()) {
			log.info("Using custom S3 presigner endpoint: {}", awsEndpoint);
			builder.endpointOverride(URI.create(awsEndpoint))
					.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
		}

		return builder.build();
	}
}



