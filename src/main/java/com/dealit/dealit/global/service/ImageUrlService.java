package com.dealit.dealit.global.service;

import com.dealit.dealit.global.config.ImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ImageUrlService {

	private static final String LEGACY_CDN_BASE_URL = "https://cdn.dealit.local";

	private final ImageProperties imageProperties;

	public String toAuctionImagePath(String storedFileName) {
		return imageProperties.auctionImagePath(storedFileName);
	}

	public String toAuctionImageUrl(String storedFileName) {
		return toPublicUrl(toAuctionImagePath(storedFileName));
	}

	public String toProfileImagePath(String storedFileName) {
		return imageProperties.profileImagePath(storedFileName);
	}

	public String toProfileImageUrl(String storedFileName) {
		return toPublicUrl(toProfileImagePath(storedFileName));
	}

	public String toProductImagePath(String storedFileName) {
		return imageProperties.productImagePath(storedFileName);
	}

	public String toProductImageUrl(String storedFileName) {
		return toPublicUrl(toProductImagePath(storedFileName));
	}

	public String toPublicUrl(String imagePathOrUrl) {
		String normalizedPath = normalizePath(imagePathOrUrl);
		if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
			return normalizedPath;
		}
		return imageProperties.normalizedPublicBaseUrl() + UriUtils.encodePath(normalizedPath, StandardCharsets.UTF_8);
	}

	public String toStoragePath(String imagePathOrUrl) {
		String normalizedPath = normalizePath(imagePathOrUrl);
		String publicBaseUrl = imageProperties.normalizedPublicBaseUrl();

		if (normalizedPath.startsWith(publicBaseUrl)) {
			return normalizedPath.substring(publicBaseUrl.length());
		}
		if (normalizedPath.startsWith(LEGACY_CDN_BASE_URL)) {
			return normalizedPath.substring(LEGACY_CDN_BASE_URL.length());
		}

		return normalizedPath;
	}

	private String normalizePath(String imagePathOrUrl) {
		if (imagePathOrUrl == null || imagePathOrUrl.isBlank()) {
			throw new IllegalArgumentException("imagePathOrUrl must not be blank");
		}

		if (imagePathOrUrl.startsWith("http://") || imagePathOrUrl.startsWith("https://")) {
			if (imagePathOrUrl.startsWith(LEGACY_CDN_BASE_URL)) {
				return imagePathOrUrl.substring(LEGACY_CDN_BASE_URL.length());
			}
			return imagePathOrUrl;
		}

		return imagePathOrUrl.startsWith("/") ? imagePathOrUrl : "/" + imagePathOrUrl;
	}
}
