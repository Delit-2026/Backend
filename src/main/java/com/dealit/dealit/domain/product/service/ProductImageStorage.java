package com.dealit.dealit.domain.product.service;

import com.dealit.dealit.domain.product.exception.InvalidProductRequestException;
import com.dealit.dealit.global.config.ImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class ProductImageStorage {

	private final ImageProperties imageProperties;

	public String store(Long imageId, MultipartFile file, String originalFilename) {
		String storedFileName = imageId + "-" + sanitizeFilename(originalFilename);
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

	public void delete(String imagePath) {
		Path relativePath = Path.of(imagePath.replaceFirst("^/+", ""));
		Path targetFile = imageProperties.productImageDirectory().getParent().getParent().resolve(relativePath);

		try {
			Files.deleteIfExists(targetFile);
		} catch (IOException exception) {
			throw new InvalidProductRequestException("이미지 파일 삭제에 실패했습니다.");
		}
	}

	private String sanitizeFilename(String originalFilename) {
		String fileNameOnly = Path.of(originalFilename).getFileName().toString().trim();
		String normalized = fileNameOnly
			.replaceAll("\\s+", "-")
			.replaceAll("[^\\p{L}\\p{N}._()-]", "-")
			.replaceAll("-{2,}", "-");

		if (normalized.isBlank()) {
			return "image.jpg";
		}

		return normalized;
	}
}
