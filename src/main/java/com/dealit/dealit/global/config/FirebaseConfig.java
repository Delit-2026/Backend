package com.dealit.dealit.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

	@Bean
	@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
	public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
		if (!FirebaseApp.getApps().isEmpty()) {
			return FirebaseApp.getInstance();
		}

		FirebaseOptions.Builder builder = FirebaseOptions.builder()
			.setCredentials(loadCredentials(properties));

		if (properties.projectId() != null && !properties.projectId().isBlank()) {
			builder.setProjectId(properties.projectId().trim());
		}

		return FirebaseApp.initializeApp(builder.build());
	}

	@Bean
	@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
	public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
		return FirebaseMessaging.getInstance(firebaseApp);
	}

	private GoogleCredentials loadCredentials(FirebaseProperties properties) throws IOException {
		if (properties.serviceAccountBase64() != null && !properties.serviceAccountBase64().isBlank()) {
			byte[] decoded = Base64.getDecoder().decode(properties.serviceAccountBase64().trim());
			return fromBytes(decoded);
		}

		if (properties.serviceAccountJson() != null && !properties.serviceAccountJson().isBlank()) {
			return fromBytes(properties.serviceAccountJson().getBytes(StandardCharsets.UTF_8));
		}

		if (properties.serviceAccountPath() != null && !properties.serviceAccountPath().isBlank()) {
			try (InputStream inputStream = new FileInputStream(properties.serviceAccountPath().trim())) {
				return GoogleCredentials.fromStream(inputStream);
			}
		}

		return GoogleCredentials.getApplicationDefault();
	}

	private GoogleCredentials fromBytes(byte[] bytes) throws IOException {
		try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
			return GoogleCredentials.fromStream(inputStream);
		}
	}
}
