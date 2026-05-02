package com.dealit.dealit.domain.member.service;

import com.dealit.dealit.domain.member.exception.InvalidProfileRequestException;
import com.dealit.dealit.global.config.ImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileImageStorage {

	private static final long MAX_PROFILE_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/jpeg",
		"image/png",
		"image/webp"
	);

	private final ImageProperties imageProperties;

	public String store(Long memberId, MultipartFile file) {
		validate(file);

		String originalFilename = file.getOriginalFilename() == null ? "profile.jpg" : file.getOriginalFilename();
		String storedFileName = memberId + "-" + UUID.randomUUID() + resolveExtension(originalFilename, file.getContentType());
		Path directory = imageProperties.profileImageDirectory();
		Path targetFile = directory.resolve(storedFileName);

		try {
			Files.createDirectories(directory);
			file.transferTo(targetFile);
		} catch (IOException exception) {
			throw new InvalidProfileRequestException("프로필 이미지 저장에 실패했습니다.");
		}

		return storedFileName;
	}

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidProfileRequestException("프로필 이미지 파일은 필수입니다.");
		}
		if (file.getSize() > MAX_PROFILE_IMAGE_SIZE_BYTES) {
			throw new InvalidProfileRequestException("프로필 이미지는 5MB 이하만 업로드할 수 있습니다.");
		}
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			throw new InvalidProfileRequestException("프로필 이미지는 jpg, jpeg, png, webp 형식만 업로드할 수 있습니다.");
		}
	}

	private String resolveExtension(String originalFilename, String contentType) {
		String normalizedFilename = originalFilename == null
			? ""
			: Normalizer.normalize(Path.of(originalFilename).getFileName().toString().trim(), Normalizer.Form.NFC);
		int dotIndex = normalizedFilename.lastIndexOf('.');
		if (dotIndex >= 0 && dotIndex < normalizedFilename.length() - 1) {
			String extension = normalizedFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
			if (Set.of("jpg", "jpeg", "png", "webp").contains(extension)) {
				return "." + extension;
			}
		}

		return switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> ".jpg";
		};
	}
}
