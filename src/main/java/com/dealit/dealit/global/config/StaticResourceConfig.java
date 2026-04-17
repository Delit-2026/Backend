package com.dealit.dealit.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class StaticResourceConfig implements WebMvcConfigurer {

	private final ImageProperties imageProperties;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler(ImageProperties.AUCTION_IMAGE_PATH_PREFIX + "**")
			.addResourceLocations(imageProperties.auctionImageDirectory().toUri().toString());

		registry.addResourceHandler(ImageProperties.PROFILE_IMAGE_PATH_PREFIX + "**")
			.addResourceLocations(imageProperties.profileImageDirectory().toUri().toString());
	}
}
