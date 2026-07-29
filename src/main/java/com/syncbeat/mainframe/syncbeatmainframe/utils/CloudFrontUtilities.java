package com.syncbeat.mainframe.syncbeatmainframe.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * Utility class for generating CloudFront signed URLs for secure content delivery.
 * Uses CloudFront key pairs for canned policy signing.
 *
 * Implements RSA-SHA1 signing compatible with CloudFront's URL signing requirements.
 */
@Slf4j
@Component
public class CloudFrontUtilities {

	@Value("${cloudfront.domain-name:}")
	private String cloudFrontDomainName;

	@Value("${cloudfront.key-pair-id:}")
	private String keyPairId;

	@Value("${cloudfront.private-key-path:}")
	private String privateKeyPath;

	@Value("${cloudfront.expiration-seconds:3600}")
	private long expirationSeconds;

	/**
	 * Generates a signed CloudFront URL with a canned policy.
	 * The URL will be valid for the specified expiration time.
	 *
	 * @param resourcePath The S3 path (e.g., "tracks/track-id/original.mp3")
	 * @return A signed CloudFront URL
	 * @throws IOException If the private key cannot be read
	 * @throws Exception If URL signing fails
	 */
	public String getSignedUrlWithCannedPolicy(String resourcePath) throws IOException, Exception {
		if (cloudFrontDomainName == null || cloudFrontDomainName.isEmpty()) {
			throw new IllegalArgumentException("CloudFront domain name is not configured");
		}

		if (keyPairId == null || keyPairId.isEmpty()) {
			throw new IllegalArgumentException("CloudFront key pair ID is not configured");
		}

		if (privateKeyPath == null || privateKeyPath.isEmpty()) {
			throw new IllegalArgumentException("CloudFront private key path is not configured");
		}

		// Construct the full CloudFront URL
		String cloudFrontUrl = "https://" + cloudFrontDomainName + "/" + resourcePath;

		// Calculate expiration time (current time + expiration seconds)
		long expirationTime = Instant.now().getEpochSecond() + expirationSeconds;

		try {
			// Read the private key from the file
			byte[] privateKeyBytes = readPrivateKeyBytes(privateKeyPath);

			// Load private key
			PrivateKey privateKey = loadPrivateKey(privateKeyBytes);

			// Create the canned policy JSON
			String policy = String.format(
					"{\"Statement\":[{\"Resource\":\"%s\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":%d}}}]}",
					cloudFrontUrl,
					expirationTime
			);

			// Sign the policy
			byte[] signature = signPolicy(policy.getBytes(StandardCharsets.UTF_8), privateKey);

			// Encode signature and policy
			String encodedSignature = encodeSignature(signature);
			String encodedPolicy = Base64.getUrlEncoder().withoutPadding().encodeToString(
					policy.getBytes(StandardCharsets.UTF_8)
			);

			// Build the signed URL
			String signedUrl = String.format(
					"%s?Policy=%s&Signature=%s&Key-Pair-Id=%s",
					cloudFrontUrl,
					encodedPolicy,
					encodedSignature,
					keyPairId
			);

			log.info("Generated signed CloudFront URL for resource: {}", resourcePath);
			return signedUrl;
		} catch (IOException e) {
			log.error("Error reading private key file: {}", privateKeyPath, e);
			throw e;
		} catch (Exception e) {
			log.error("Error signing CloudFront URL: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Reads private key bytes from a file.
	 * Handles both PEM-formatted and raw DER-formatted keys.
	 *
	 * @param keyPath Path to the private key file
	 * @return Byte array of the key
	 * @throws IOException If the file cannot be read
	 */
	private byte[] readPrivateKeyBytes(String keyPath) throws IOException {
		byte[] keyBytes = Files.readAllBytes(Paths.get(keyPath));

		// If it's a PEM file, extract the DER content
		String keyString = new String(keyBytes, StandardCharsets.UTF_8);
		if (keyString.contains("BEGIN")) {
			// Remove PEM headers and whitespace
			keyString = keyString
					.replace("-----BEGIN PRIVATE KEY-----", "")
					.replace("-----END PRIVATE KEY-----", "")
					.replace("-----BEGIN RSA PRIVATE KEY-----", "")
					.replace("-----END RSA PRIVATE KEY-----", "")
					.replaceAll("\\s", "");
			keyBytes = Base64.getDecoder().decode(keyString);
		}

		return keyBytes;
	}

	/**
	 * Loads a private key from DER-encoded bytes.
	 *
	 * @param keyBytes DER-encoded private key bytes
	 * @return PrivateKey object
	 * @throws Exception If the key cannot be loaded
	 */
	private PrivateKey loadPrivateKey(byte[] keyBytes) throws Exception {
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePrivate(keySpec);
	}

	/**
	 * Signs a policy string using RSA-SHA1.
	 *
	 * @param policyBytes The policy string as bytes
	 * @param privateKey The private key to use for signing
	 * @return Signature bytes
	 * @throws Exception If signing fails
	 */
	private byte[] signPolicy(byte[] policyBytes, PrivateKey privateKey) throws Exception {
		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(privateKey);
		signature.update(policyBytes);
		return signature.sign();
	}

	/**
	 * Encodes the signature for use in a URL, using base64 URL-safe encoding.
	 *
	 * @param signatureBytes The raw signature bytes
	 * @return URL-safe base64 encoded signature
	 */
	private String encodeSignature(byte[] signatureBytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
	}

	/**
	 * Gets the expiration timestamp for signed URLs.
	 *
	 * @return Unix timestamp (seconds since epoch)
	 */
	public long getExpirationTimestamp() {
		return Instant.now().getEpochSecond() + expirationSeconds;
	}
}




