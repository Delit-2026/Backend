package com.dealit.dealit.domain.auction.service;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.ProductSaleType;
import com.dealit.dealit.domain.auction.dto.CreateAuctionRequest;
import com.dealit.dealit.domain.auction.dto.CreateAuctionResponse;
import com.dealit.dealit.domain.auction.dto.DeleteAuctionImageResponse;
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
import com.dealit.dealit.global.service.ImageUrlService;
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

	private final AuctionProductRepository auctionProductRepository;
	private final AuctionProductImageRepository auctionProductImageRepository;
	private final AuctionDraftRepository auctionDraftRepository;
	private final AuctionImageStorage auctionImageStorage;
	private final ImageUrlService imageUrlService;
	private final ObjectMapper objectMapper;

	@Transactional
	public UploadAuctionImageResponse uploadImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidAuctionRequestException("이미지 파일은 필수입니다.");
		}

		String originalFilename = file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename().trim();
		AuctionProductImage savedImage = auctionProductImageRepository.save(
			AuctionProductImage.createTemporary("", originalFilename)
		);

		String storedFileName = auctionImageStorage.store(savedImage.getImageId(), file, originalFilename);
		savedImage.updateImageUrl(imageUrlService.toAuctionImagePath(storedFileName));
		return new UploadAuctionImageResponse(
			savedImage.getImageId(),
			imageUrlService.toPublicUrl(savedImage.getImageUrl())
		);
	}

	@Transactional
	public DeleteAuctionImageResponse deleteImage(Long imageId) {
		AuctionProductImage image = auctionProductImageRepository.findByImageIdAndDeletedAtIsNull(imageId)
			.orElseThrow(() -> new AuctionImageNotFoundException("존재하지 않는 이미지입니다."));

		if (image.getProduct() != null) {
			throw new InvalidAuctionRequestException("이미 상품에 연결된 이미지는 삭제할 수 없습니다.");
		}

		auctionImageStorage.delete(image.getImageUrl());
		image.softDelete();
		return new DeleteAuctionImageResponse(image.getImageId(), true);
	}

	@Transactional
	public SaveAuctionDraftResponse saveDraft(SaveAuctionDraftRequest request) {
		validateDraftRequest(request);

		OffsetDateTime savedAt = OffsetDateTime.now(ZoneOffset.UTC);
		String payloadJson = serializeDraft(request);

		AuctionDraft draft = request.draftId() == null
			? AuctionDraft.create(
				normalizeBlank(request.name()),
				normalizeBlank(request.description()),
				request.saleType(),
				request.categoryId(),
				request.price(),
				request.startPrice(),
				request.auctionStartAt(),
				request.auctionEndAt(),
				normalizeBlank(request.location()),
				payloadJson,
				savedAt
			)
			: auctionDraftRepository.findById(request.draftId())
				.map(existingDraft -> {
					existingDraft.update(
						normalizeBlank(request.name()),
						normalizeBlank(request.description()),
						request.saleType(),
						request.categoryId(),
						request.price(),
						request.startPrice(),
						request.auctionStartAt(),
						request.auctionEndAt(),
						normalizeBlank(request.location()),
						payloadJson,
						savedAt
					);
					return existingDraft;
				})
				.orElseGet(() -> AuctionDraft.create(
					normalizeBlank(request.name()),
					normalizeBlank(request.description()),
					request.saleType(),
					request.categoryId(),
					request.price(),
					request.startPrice(),
					request.auctionStartAt(),
					request.auctionEndAt(),
					normalizeBlank(request.location()),
					payloadJson,
					savedAt
				));

		AuctionDraft savedDraft = auctionDraftRepository.save(draft);
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
				request.auctionStartAt(),
				request.auctionEndAt(),
				request.location().trim(),
				request.draftId(),
				determineStatus(
					request.saleType(),
					request.auctionStartAt(),
					request.auctionEndAt(),
					OffsetDateTime.now(ZoneOffset.UTC)
				)
			)
		);

		Map<Long, AuctionProductImage> imagesById = loadRequestedImages(request.images());
		for (ProductImagePayload imagePayload : request.images()) {
			AuctionProductImage image = imagesById.get(imagePayload.imageId());
			product.attachImage(image, imagePayload.sortOrder());
		}
		auctionProductImageRepository.saveAll(imagesById.values());

		CreateAuctionResponse.AuctionScheduleResponse auction = product.getSaleType() == ProductSaleType.AUCTION
			? buildAuctionScheduleResponse(product)
			: null;

		return new CreateAuctionResponse(
			product.getProductId(),
			product.getSaleType(),
			product.getStatus(),
			auction
		);
	}

	private Map<Long, AuctionProductImage> loadRequestedImages(List<ProductImagePayload> imagePayloads) {
		List<Long> imageIds = imagePayloads.stream()
			.map(ProductImagePayload::imageId)
			.toList();

		List<AuctionProductImage> images = auctionProductImageRepository.findAllByImageIdInAndDeletedAtIsNull(imageIds);
		if (images.size() != imageIds.size()) {
			throw new AuctionImageNotFoundException("존재하지 않는 이미지가 포함되어 있습니다.");
		}

		Map<Long, AuctionProductImage> imagesById = new LinkedHashMap<>();
		for (AuctionProductImage image : images) {
			if (image.getProduct() != null) {
				throw new InvalidAuctionRequestException("이미 다른 상품에 연결된 이미지가 포함되어 있습니다.");
			}
			imagesById.put(image.getImageId(), image);
		}

		validateDuplicateImageIds(imageIds, imagesById.keySet());
		return imagesById;
	}

	private CreateAuctionResponse.AuctionScheduleResponse buildAuctionScheduleResponse(AuctionProduct product) {
		if (product.getAuctionStartAt() == null || product.getAuctionEndAt() == null) {
			throw new InvalidAuctionRequestException("경매 응답에는 시작 시각과 종료 시각이 반드시 포함되어야 합니다.");
		}

		return new CreateAuctionResponse.AuctionScheduleResponse(
			product.getStatus(),
			product.getAuctionStartAt(),
			product.getAuctionEndAt()
		);
	}

	private void validateDuplicateImageIds(List<Long> imageIds, Collection<Long> storedImageIds) {
		if (storedImageIds.size() != imageIds.size()) {
			throw new InvalidAuctionRequestException("중복된 이미지 ID는 허용되지 않습니다.");
		}
	}

	private void validateRequestBySaleType(CreateAuctionRequest request) {
		if (request.saleType() == ProductSaleType.AUCTION) {
			if (request.startPrice() == null) {
				throw new InvalidAuctionRequestException("경매 판매에서는 시작가가 필수입니다.");
			}
			if (request.auctionStartAt() == null) {
				throw new InvalidAuctionRequestException("경매 판매에서는 시작 시각이 필수입니다.");
			}
			if (request.auctionEndAt() == null) {
				throw new InvalidAuctionRequestException("경매 판매에서는 종료 시각이 필수입니다.");
			}
			if (request.startPrice().signum() <= 0) {
				throw new InvalidAuctionRequestException("시작가는 0보다 커야 합니다.");
			}
			if (!request.auctionStartAt().isBefore(request.auctionEndAt())) {
				throw new InvalidAuctionRequestException("경매 시작 시각은 종료 시각보다 빨라야 합니다.");
			}
			return;
		}

		if (request.price() == null) {
			throw new InvalidAuctionRequestException("일반 판매에서는 판매가가 필수입니다.");
		}
		if (request.price().signum() <= 0) {
			throw new InvalidAuctionRequestException("판매가는 0보다 커야 합니다.");
		}
	}

	private AuctionStatus determineStatus(
		ProductSaleType saleType,
		OffsetDateTime auctionStartAt,
		OffsetDateTime auctionEndAt,
		OffsetDateTime now
	) {
		if (saleType == ProductSaleType.REGULAR) {
			return AuctionStatus.ON_SALE;
		}
		if (auctionStartAt != null && auctionStartAt.isAfter(now)) {
			return AuctionStatus.AUCTION_SCHEDULED;
		}
		if (auctionEndAt != null && auctionEndAt.isBefore(now)) {
			throw new InvalidAuctionRequestException("경매 종료 시각은 현재 시각 이후여야 합니다.");
		}
		return AuctionStatus.AUCTION_LIVE;
	}

	private String serializeDraft(SaveAuctionDraftRequest request) {
		try {
			return objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException exception) {
			throw new InvalidAuctionRequestException("임시저장 데이터를 처리하는 중 오류가 발생했습니다.");
		}
	}

	private void validateDraftRequest(SaveAuctionDraftRequest request) {
		boolean hasSaleType = request.saleType() != null;
		boolean hasName = normalizeBlank(request.name()) != null;
		boolean hasDescription = normalizeBlank(request.description()) != null;
		boolean hasCategory = request.categoryId() != null;
		boolean hasPrice = request.price() != null;
		boolean hasStartPrice = request.startPrice() != null;
		boolean hasAuctionStartAt = request.auctionStartAt() != null;
		boolean hasAuctionEndAt = request.auctionEndAt() != null;
		boolean hasImages = request.images() != null && !request.images().isEmpty();
		boolean hasLocation = normalizeBlank(request.location()) != null;

		if (
			!hasSaleType &&
			!hasName &&
			!hasDescription &&
			!hasCategory &&
			!hasPrice &&
			!hasStartPrice &&
			!hasAuctionStartAt &&
			!hasAuctionEndAt &&
			!hasImages &&
			!hasLocation
		) {
			throw new InvalidAuctionRequestException("임시저장할 내용이 없습니다.");
		}
	}

	private String normalizeBlank(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
