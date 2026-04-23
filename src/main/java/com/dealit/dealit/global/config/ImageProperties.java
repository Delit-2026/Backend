package com.dealit.dealit.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties(prefix = "app.images")
public record ImageProperties(
	String publicBaseUrl,
	String storageRoot
) {

	public static final String AUCTION_IMAGE_PATH_PREFIX = "/auction/images/";
	public static final String PROFILE_IMAGE_PATH_PREFIX = "/profile/images/";

	public Path auctionImageDirectory() {
		return Paths.get(storageRoot).resolve("auction").resolve("images");
	}

	public Path profileImageDirectory() {
		return Paths.get(storageRoot).resolve("profile").resolve("images");
	}

	public String auctionImagePath(String storedFileName) {
		return AUCTION_IMAGE_PATH_PREFIX + storedFileName;
	}

	public String profileImagePath(String storedFileName) {
		return PROFILE_IMAGE_PATH_PREFIX + storedFileName;
	}

	public String normalizedPublicBaseUrl() {
		if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
			throw new IllegalStateException("app.images.public-base-url must not be blank");
		}

		return publicBaseUrl.endsWith("/")
			? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
			: publicBaseUrl;
	}
}
