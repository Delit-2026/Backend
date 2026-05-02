package com.dealit.dealit.domain.product.service;

import com.dealit.dealit.domain.product.exception.InvalidProductRequestException;
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
public class ProductImageStorage {

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/jpeg",
		"image/png",
		"image/webp"
	);

	private final ImageProperties imageProperties;

	public String store(Long imageId, MultipartFile file, String originalFilename) {
		validate(file);

		String storedFileName = imageId + "-" + UUID.randomUUID() + resolveExtension(originalFilename, file.getContentType());
		Path directory = imageProperties.productImageDirectory();
		Path targetFile = directory.resolve(storedFileName);

		try {
			Files.createDirectories(directory);
			file.transferTo(targetFile);
		} catch (IOException exception) {
			throw new InvalidProductRequestException("이미지 파일 저장에 실패했습니다.");
		}

		return storedFileName;
	}

	private void validate(MultipartFile file) {
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			throw new InvalidProductRequestException("상품 이미지는 jpg, jpeg, png, webp 형식만 업로드할 수 있습니다.");
		}
	}

	public void delete(String imagePath) {
		Path relativePath = Path.of(imagePath.replaceFirst("^/+", "").replaceFirst("^uploads/", ""));
		Path targetFile = imageProperties.productImageDirectory().getParent().getParent().resolve(relativePath);

		try {
			Files.deleteIfExists(targetFile);
		} catch (IOException exception) {
			throw new InvalidProductRequestException("이미지 파일 삭제에 실패했습니다.");
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
