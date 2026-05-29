package com.dealit.dealit.global.service;

import com.dealit.dealit.global.config.ImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageUrlService {

	private static final String LEGACY_CDN_BASE_URL = "https://cdn.dealit.local";
	private static final List<String> MANAGED_IMAGE_PATH_PREFIXES = List.of(
		ImageProperties.AUCTION_IMAGE_PATH_PREFIX,
		ImageProperties.PRODUCT_IMAGE_PATH_PREFIX,
		ImageProperties.PROFILE_IMAGE_PATH_PREFIX,
		ImageProperties.LEGACY_AUCTION_IMAGE_PATH_PREFIX,
		ImageProperties.LEGACY_PRODUCT_IMAGE_PATH_PREFIX,
		ImageProperties.LEGACY_PROFILE_IMAGE_PATH_PREFIX
	);

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
			String managedImagePath = extractManagedImagePath(imagePathOrUrl);
			if (managedImagePath != null) {
				return managedImagePath;
			}
			return imagePathOrUrl;
		}

		return imagePathOrUrl.startsWith("/") ? imagePathOrUrl : "/" + imagePathOrUrl;
	}

	private String extractManagedImagePath(String imageUrl) {
		try {
			URI uri = new URI(imageUrl);
			String path = uri.getPath();
			if (path == null || path.isBlank()) {
				return null;
			}
			return isManagedImagePath(path) ? path : null;
		} catch (URISyntaxException exception) {
			return null;
		}
	}

	private boolean isManagedImagePath(String path) {
		return MANAGED_IMAGE_PATH_PREFIXES.stream().anyMatch(path::startsWith);
	}
}
