package com.dealit.dealit.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.firebase")
public record FirebaseProperties(
	boolean enabled,
	String projectId,
	String serviceAccountPath,
	String serviceAccountJson,
	String serviceAccountBase64
) {
}
