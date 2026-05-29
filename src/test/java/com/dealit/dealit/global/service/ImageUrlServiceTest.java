package com.dealit.dealit.global.service;

import com.dealit.dealit.global.config.ImageProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUrlServiceTest {

	private final ImageUrlService imageUrlService = new ImageUrlService(new ImageProperties(
		"https://api.dealit.site",
		"/tmp/dealit-uploads",
		"s3",
		"dealit-bucket",
		"ap-northeast-2",
		"",
		""
	));

	@Test
	void toPublicUrlRewritesManagedLocalhostImageUrlToCurrentPublicBaseUrl() {
		String result = imageUrlService.toPublicUrl(
			"http://localhost:8080/uploads/product/images/1-sample.jpg"
		);

		assertThat(result).isEqualTo("https://api.dealit.site/uploads/product/images/1-sample.jpg");
	}

	@Test
	void toPublicUrlRewritesLegacyManagedImageUrlToCurrentPublicBaseUrl() {
		String result = imageUrlService.toPublicUrl(
			"http://localhost:8080/product/images/1-sample.jpg"
		);

		assertThat(result).isEqualTo("https://api.dealit.site/product/images/1-sample.jpg");
	}

	@Test
	void toPublicUrlLeavesExternalImageUrlUnchanged() {
		String result = imageUrlService.toPublicUrl(
			"https://example.com/assets/image.jpg"
		);

		assertThat(result).isEqualTo("https://example.com/assets/image.jpg");
	}
}
