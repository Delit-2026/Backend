package com.dealit.dealit.global.service;

import com.dealit.dealit.global.config.ImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class S3ImageStorageClient {

	private final ImageProperties imageProperties;

	public void upload(String key, MultipartFile file) throws IOException {
		try (S3Client s3Client = s3Client()) {
			PutObjectRequest request = PutObjectRequest.builder()
				.bucket(requiredBucket())
				.key(normalizeKey(key))
				.contentType(file.getContentType())
				.contentLength(file.getSize())
				.build();

			s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
		}
	}

	public void delete(String imagePathOrUrl) {
		try (S3Client s3Client = s3Client()) {
			DeleteObjectRequest request = DeleteObjectRequest.builder()
				.bucket(requiredBucket())
				.key(toKey(imagePathOrUrl))
				.build();

			s3Client.deleteObject(request);
		}
	}

	public String toKey(String imagePathOrUrl) {
		String value = imagePathOrUrl == null ? "" : imagePathOrUrl.trim();
		String publicBaseUrl = imageProperties.normalizedPublicBaseUrl();

		if (value.startsWith(publicBaseUrl)) {
			value = value.substring(publicBaseUrl.length());
		}

		int queryIndex = value.indexOf('?');
		if (queryIndex >= 0) {
			value = value.substring(0, queryIndex);
		}

		return normalizeKey(value);
	}

	private S3Client s3Client() {
		return S3Client.builder()
			.region(Region.of(requiredRegion()))
			.credentialsProvider(credentialsProvider())
			.build();
	}

	private AwsCredentialsProvider credentialsProvider() {
		String accessKeyId = imageProperties.s3AccessKeyId();
		String secretAccessKey = imageProperties.s3SecretAccessKey();
		if (hasText(accessKeyId) && hasText(secretAccessKey)) {
			return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
		}
		return DefaultCredentialsProvider.create();
	}

	private String requiredBucket() {
		String bucket = imageProperties.s3Bucket();
		if (!hasText(bucket)) {
			throw new IllegalStateException("AWS_S3_BUCKET must not be blank when S3 image storage is enabled.");
		}
		return bucket;
	}

	private String requiredRegion() {
		String region = imageProperties.s3Region();
		if (!hasText(region)) {
			throw new IllegalStateException("AWS_REGION must not be blank when S3 image storage is enabled.");
		}
		return region;
	}

	private String normalizeKey(String key) {
		return key == null ? "" : key.replaceFirst("^/+", "");
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
