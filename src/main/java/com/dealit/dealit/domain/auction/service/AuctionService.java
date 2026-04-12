package com.dealit.dealit.domain.auction.service;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.ProductSaleType;
import com.dealit.dealit.domain.auction.dto.CreateAuctionRequest;
import com.dealit.dealit.domain.auction.dto.CreateAuctionResponse;
import com.dealit.dealit.domain.auction.dto.ProductImagePayload;
import com.dealit.dealit.domain.auction.dto.RecommendCategoryRequest;
import com.dealit.dealit.domain.auction.dto.RecommendCategoryResponse;
import com.dealit.dealit.domain.auction.dto.RecommendPriceRequest;
import com.dealit.dealit.domain.auction.dto.RecommendPriceResponse;
import com.dealit.dealit.domain.auction.dto.SaveAuctionDraftRequest;
import com.dealit.dealit.domain.auction.dto.SaveAuctionDraftResponse;
import com.dealit.dealit.domain.auction.dto.UploadAuctionImageResponse;
import com.dealit.dealit.domain.auction.entity.AuctionDraft;
import com.dealit.dealit.domain.auction.entity.AuctionProduct;
import com.dealit.dealit.domain.auction.entity.AuctionProductImage;
import com.dealit.dealit.domain.auction.exception.AuctionImageNotFoundException;
import com.dealit.dealit.domain.auction.exception.InvalidAuctionRequestException;
import com.dealit.dealit.domain.auction.repository.AuctionDraftRepository;
import com.dealit.dealit.domain.auction.repository.AuctionProductImageRepository;
import com.dealit.dealit.domain.auction.repository.AuctionProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuctionService {

	private static final String IMAGE_BASE_URL = "https://cdn.dealit.local/auction/images/";

	private final AuctionProductRepository auctionProductRepository;
	private final AuctionProductImageRepository auctionProductImageRepository;
	private final AuctionDraftRepository auctionDraftRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public UploadAuctionImageResponse uploadImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidAuctionRequestException("file is required.");
		}

		String originalFilename = file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename().trim();
		String sanitizedFilename = originalFilename.replaceAll("\\s+", "-");
		AuctionProductImage savedImage = auctionProductImageRepository.saveAndFlush(
			AuctionProductImage.createTemporary(IMAGE_BASE_URL + sanitizedFilename, originalFilename)
		);

		String imageUrl = IMAGE_BASE_URL + savedImage.getImageId() + "-" + sanitizedFilename;
		savedImage.updateImageUrl(imageUrl);
		return new UploadAuctionImageResponse(savedImage.getImageId(), imageUrl);
	}

	@Transactional
	public SaveAuctionDraftResponse saveDraft(SaveAuctionDraftRequest request) {
		OffsetDateTime savedAt = OffsetDateTime.now(ZoneOffset.UTC);
		String payloadJson = serializeDraft(request);

		AuctionDraft draft = request.draftId() == null
			? AuctionDraft.create(
				request.name().trim(),
				request.description().trim(),
				request.saleType(),
				request.categoryId(),
				request.price(),
				request.startPrice(),
				request.auctionEndAt(),
				request.allowOffer(),
				request.location().trim(),
				payloadJson,
				savedAt
			)
			: auctionDraftRepository.findById(request.draftId())
				.map(existingDraft -> {
					existingDraft.update(
						request.name().trim(),
						request.description().trim(),
						request.saleType(),
						request.categoryId(),
						request.price(),
						request.startPrice(),
						request.auctionEndAt(),
						request.allowOffer(),
						request.location().trim(),
						payloadJson,
						savedAt
					);
					return existingDraft;
				})
				.orElseGet(() -> AuctionDraft.create(
					request.name().trim(),
					request.description().trim(),
					request.saleType(),
					request.categoryId(),
					request.price(),
					request.startPrice(),
					request.auctionEndAt(),
					request.allowOffer(),
					request.location().trim(),
					payloadJson,
					savedAt
				));

		AuctionDraft savedDraft = auctionDraftRepository.saveAndFlush(draft);
		return new SaveAuctionDraftResponse(savedDraft.getDraftId(), savedDraft.getSavedAt());
	}

	@Transactional(readOnly = true)
	public RecommendCategoryResponse recommendCategory(RecommendCategoryRequest request) {
		String combined = (request.name() + " " + request.description()).toLowerCase();
		if (combined.contains("watch") || combined.contains("iphone") || combined.contains("switch")) {
			return new RecommendCategoryResponse(200L, "Digital/Electronics");
		}
		if (combined.contains("chair") || combined.contains("table")) {
			return new RecommendCategoryResponse(300L, "Furniture/Interior");
		}
		return new RecommendCategoryResponse(999L, "Others");
	}

	@Transactional(readOnly = true)
	public RecommendPriceResponse recommendPrice(RecommendPriceRequest request) {
		int textWeight = request.name().trim().length() * 10 + request.description().trim().length() * 2;
		BigDecimal recommendedPrice = BigDecimal.valueOf(Math.max(10000, textWeight * 100L));
		if (request.saleType() == ProductSaleType.AUCTION) {
			BigDecimal startPrice = recommendedPrice.multiply(BigDecimal.valueOf(0.7)).setScale(0, RoundingMode.DOWN);
			return new RecommendPriceResponse(recommendedPrice, startPrice);
		}
		return new RecommendPriceResponse(recommendedPrice, null);
	}

	@Transactional
	public CreateAuctionResponse createAuction(CreateAuctionRequest request) {
		validateRequestBySaleType(request);

		AuctionProduct product = auctionProductRepository.save(
			AuctionProduct.create(
				request.name().trim(),
				request.description().trim(),
				request.saleType(),
				request.categoryId(),
				request.price(),
				request.startPrice(),
				request.auctionEndAt(),
				request.allowOffer(),
				request.location().trim(),
				request.draftId(),
				determineStatus(request.saleType(), request.auctionEndAt(), OffsetDateTime.now(ZoneOffset.UTC))
			)
		);

		Map<Long, AuctionProductImage> imagesById = loadRequestedImages(request.images());
		for (ProductImagePayload imagePayload : request.images()) {
			AuctionProductImage image = imagesById.get(imagePayload.imageId());
			product.attachImage(image, imagePayload.sortOrder());
		}
		auctionProductImageRepository.saveAll(imagesById.values());

		return new CreateAuctionResponse(product.getProductId(), product.getSaleType(), product.getStatus());
	}

	private Map<Long, AuctionProductImage> loadRequestedImages(List<ProductImagePayload> imagePayloads) {
		List<Long> imageIds = imagePayloads.stream()
			.map(ProductImagePayload::imageId)
			.toList();

		List<AuctionProductImage> images = auctionProductImageRepository.findAllByImageIdInAndDeletedAtIsNull(imageIds);
		if (images.size() != imageIds.size()) {
			throw new AuctionImageNotFoundException("One or more images do not exist.");
		}

		Map<Long, AuctionProductImage> imagesById = new LinkedHashMap<>();
		for (AuctionProductImage image : images) {
			if (image.getProduct() != null) {
				throw new InvalidAuctionRequestException("One or more images are already assigned to another product.");
			}
			imagesById.put(image.getImageId(), image);
		}

		validateDuplicateImageIds(imageIds, imagesById.keySet());
		return imagesById;
	}

	private void validateDuplicateImageIds(List<Long> imageIds, Collection<Long> storedImageIds) {
		if (storedImageIds.size() != imageIds.size()) {
			throw new InvalidAuctionRequestException("Duplicate imageId values are not allowed.");
		}
	}

	private void validateRequestBySaleType(CreateAuctionRequest request) {
		if (request.saleType() == ProductSaleType.AUCTION) {
			if (request.startPrice() == null) {
				throw new InvalidAuctionRequestException("startPrice is required when saleType is AUCTION.");
			}
			if (request.auctionEndAt() == null) {
				throw new InvalidAuctionRequestException("auctionEndAt is required when saleType is AUCTION.");
			}
			if (request.startPrice().signum() <= 0) {
				throw new InvalidAuctionRequestException("startPrice must be greater than 0.");
			}
			return;
		}

		if (request.price() == null) {
			throw new InvalidAuctionRequestException("price is required when saleType is REGULAR.");
		}
		if (request.price().signum() <= 0) {
			throw new InvalidAuctionRequestException("price must be greater than 0.");
		}
	}

	private AuctionStatus determineStatus(ProductSaleType saleType, OffsetDateTime auctionEndAt, OffsetDateTime now) {
		if (saleType == ProductSaleType.REGULAR) {
			return AuctionStatus.ON_SALE;
		}
		if (auctionEndAt != null && auctionEndAt.isBefore(now)) {
			throw new InvalidAuctionRequestException("auctionEndAt must be in the future.");
		}
		return AuctionStatus.AUCTION_LIVE;
	}

	private String serializeDraft(SaveAuctionDraftRequest request) {
		try {
			return objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException exception) {
			throw new InvalidAuctionRequestException("Failed to serialize draft payload.");
		}
	}
}
