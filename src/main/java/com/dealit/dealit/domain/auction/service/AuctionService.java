package com.dealit.dealit.domain.auction.service;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.ProductSaleType;
import com.dealit.dealit.domain.auction.dto.AuctionEditDetailResponse;
import com.dealit.dealit.domain.auction.dto.AuctionEditDetailResponse.AuctionEditImageResponse;
import com.dealit.dealit.domain.auction.dto.CategoryOptionResponse;
import com.dealit.dealit.domain.auction.dto.CreateAuctionRequest;
import com.dealit.dealit.domain.auction.dto.CreateAuctionResponse;
import com.dealit.dealit.domain.auction.dto.DeleteAuctionImageResponse;
import com.dealit.dealit.domain.auction.dto.MySellingAuctionItemResponse;
import com.dealit.dealit.domain.auction.dto.MySellingAuctionListResponse;
import com.dealit.dealit.domain.auction.dto.ProductImagePayload;
import com.dealit.dealit.domain.auction.dto.RecommendCategoryRequest;
import com.dealit.dealit.domain.auction.dto.RecommendCategoryResponse;
import com.dealit.dealit.domain.auction.dto.RecommendPriceRequest;
import com.dealit.dealit.domain.auction.dto.RecommendPriceResponse;
import com.dealit.dealit.domain.auction.dto.SaveAuctionDraftRequest;
import com.dealit.dealit.domain.auction.dto.SaveAuctionDraftResponse;
import com.dealit.dealit.domain.auction.dto.UpdateAuctionRequest;
import com.dealit.dealit.domain.auction.dto.UploadAuctionImageResponse;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.entity.AuctionDraft;
import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.auction.exception.AuctionAccessDeniedException;
import com.dealit.dealit.domain.auction.exception.AuctionConflictException;
import com.dealit.dealit.domain.auction.exception.AuctionImageNotFoundException;
import com.dealit.dealit.domain.auction.exception.AuctionProductNotFoundException;
import com.dealit.dealit.domain.auction.exception.InvalidAuctionRequestException;
import com.dealit.dealit.domain.auction.repository.AuctionRepository;
import com.dealit.dealit.domain.auction.repository.AuctionDraftRepository;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.exception.EmailNotVerifiedException;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.repository.ProductImageRepository;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.global.service.ImageUrlService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private final AuctionRepository auctionRepository;
	private final AuctionDraftRepository auctionDraftRepository;
	private final CategoryRepository categoryRepository;
	private final MemberRepository memberRepository;
	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final AuctionImageStorage auctionImageStorage;
	private final ImageUrlService imageUrlService;
	private final ObjectMapper objectMapper;

	public MySellingAuctionListResponse getMySellingAuctionProducts(Long memberId, int page, int size) {
		loadActiveMember(memberId);
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = Math.min(Math.max(size, 1), 100);
		Page<Auction> auctionPage = auctionRepository.findAllByProductMemberIdAndStatusAndDeletedAtIsNullAndProductDeletedAtIsNull(
			memberId,
			AuctionStatus.AUCTION_LIVE,
			PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
		);

		Map<Long, String> categoryNamesById = loadCategoryNamesFromAuctions(auctionPage.getContent());
		List<MySellingAuctionItemResponse> content = auctionPage.getContent()
			.stream()
			.map(auction -> toMySellingAuctionItemResponse(auction, categoryNamesById))
			.toList();

		return new MySellingAuctionListResponse(
			content,
			auctionPage.getNumber(),
			auctionPage.getSize(),
			auctionPage.getTotalElements(),
			auctionPage.hasNext()
		);
	}

	@Transactional
	public UploadAuctionImageResponse uploadImage(Long memberId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidAuctionRequestException("이미지 파일은 필수입니다.");
		}
		Member member = loadVerifiedMember(memberId);

		String originalFilename = file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename().trim();
		ProductImage savedImage = productImageRepository.save(
			ProductImage.createTemporary("", originalFilename, member.getMemberId())
		);

		String storedFileName = auctionImageStorage.store(savedImage.getImageId(), file, originalFilename);
		savedImage.updateImageUrl(imageUrlService.toAuctionImagePath(storedFileName));
		return new UploadAuctionImageResponse(
			savedImage.getImageId(),
			imageUrlService.toPublicUrl(savedImage.getImageUrl())
		);
	}

	private MySellingAuctionItemResponse toMySellingAuctionItemResponse(
		Auction auction,
		Map<Long, String> categoryNamesById
	) {
		Product product = auction.getProduct();
		int bidCount = getBidCount(auction);
		int bidderCount = getBidderCount(auction);
		boolean canChange = bidCount == 0;

		return new MySellingAuctionItemResponse(
			product.getProductId(),
			auction.getAuctionId(),
			product.getName(),
			product.getDescription(),
			categoryNamesById.getOrDefault(product.getCategoryId(), ""),
			resolveThumbnailUrl(product),
			auction.getStatus(),
			auction.getStartPrice(),
			resolveCurrentPrice(auction),
			resolveMinimumNextBidPrice(auction),
			bidCount,
			bidderCount,
			toSeoulOffsetDateTime(auction.getAuctionStartAt()),
			toSeoulOffsetDateTime(auction.getAuctionEndAt()),
			canChange,
			canChange,
			toSeoulOffsetDateTime(product.getCreatedAt()),
			toSeoulOffsetDateTime(product.getUpdatedAt() == null ? product.getCreatedAt() : product.getUpdatedAt())
		);
	}

	public AuctionEditDetailResponse getAuctionEditDetail(Long memberId, Long auctionId) {
		loadActiveMember(memberId);
		Auction auction = loadOwnedAuction(memberId, auctionId);
		Product product = auction.getProduct();
		int bidCount = getBidCount(auction);
		Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);

		List<AuctionEditImageResponse> images = product.getImages().stream()
			.filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
			.sorted(this::compareImagesBySortOrder)
			.map(image -> new AuctionEditImageResponse(
				image.getImageId(),
				imageUrlService.toPublicUrl(image.getImageUrl()),
				image.getSortOrder()
			))
			.toList();

		return new AuctionEditDetailResponse(
			product.getProductId(),
			auction.getAuctionId(),
			product.getName(),
			product.getDescription(),
			product.getCategoryId(),
			category == null ? "" : category.getNameKo(),
			product.getLocation(),
			images,
			auction.getStartPrice(),
			resolveAuctionDurationDays(auction),
			auction.getStatus(),
			bidCount,
			getBidderCount(auction),
			bidCount == 0
		);
	}

	@Transactional
	public void deleteAuction(Long memberId, Long auctionId) {
		loadActiveMember(memberId);
		Auction auction = loadOwnedAuction(memberId, auctionId);
		if (getBidCount(auction) > 0) {
			throw new AuctionConflictException("입찰자가 있는 경매는 삭제할 수 없습니다.");
		}
		for (ProductImage image : auction.getProduct().getImages()) {
			auctionImageStorage.delete(image.getImageUrl());
			image.softDelete();
		}
		auction.softDelete();
		auction.getProduct().softDelete();
	}

	@Transactional
	public AuctionEditDetailResponse updateAuction(Long memberId, Long auctionId, UpdateAuctionRequest request) {
		loadActiveMember(memberId);
		Auction auction = loadOwnedAuction(memberId, auctionId);
		Product product = auction.getProduct();
		if (getBidCount(auction) > 0) {
			throw new AuctionConflictException("입찰자가 있는 경매는 수정할 수 없습니다.");
		}

		validateUpdateAuctionRequest(request);
		validateCategorySelection(request.categoryId());

		OffsetDateTime startAt = auction.getAuctionStartAt() == null
			? OffsetDateTime.now(ZoneOffset.UTC)
			: auction.getAuctionStartAt();
		OffsetDateTime endAt = startAt.plusDays(request.auctionDurationDays());
		if (!endAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
			throw new InvalidAuctionRequestException("경매 종료 시각은 현재 시각 이후여야 합니다.");
		}

		product.updateEditableDetails(
			request.name().trim(),
			request.description().trim(),
			request.categoryId(),
			request.startPrice(),
			request.location().trim()
		);
		auction.updateEditableDetails(request.startPrice(), endAt);

		replaceAuctionImages(product, request.images());

		return getAuctionEditDetail(memberId, auctionId);
	}

	private void replaceAuctionImages(Product product, List<ProductImagePayload> imagePayloads) {
		Map<Long, ProductImage> imagesById = loadRequestedProductImagesForUpdate(product, imagePayloads);
		List<ProductImage> orderedImages = toOrderedProductImages(product, imagePayloads, imagesById);
		List<ProductImage> removedImages = product.getImages().stream()
			.filter(image -> !orderedImages.contains(image))
			.toList();

		product.replaceImages(orderedImages);
		deleteRemovedAuctionImages(removedImages);
		productImageRepository.saveAll(imagesById.values());
		productImageRepository.saveAll(removedImages);
	}

	private List<ProductImage> toOrderedProductImages(
		Product product,
		List<ProductImagePayload> imagePayloads,
		Map<Long, ProductImage> imagesById
	) {
		return imagePayloads.stream()
			.map(imagePayload -> {
				ProductImage image = imagesById.get(imagePayload.imageId());
				image.assignToProduct(product, imagePayload.sortOrder());
				return image;
			})
			.toList();
	}

	private void deleteRemovedAuctionImages(List<ProductImage> removedImages) {
		for (ProductImage removedImage : removedImages) {
			auctionImageStorage.delete(removedImage.getImageUrl());
			removedImage.softDelete();
		}
	}

	private Auction loadOwnedAuction(Long memberId, Long auctionId) {
		Auction auction = auctionRepository.findByAuctionIdAndProductMemberIdAndDeletedAtIsNullAndProductDeletedAtIsNull(auctionId, memberId)
			.orElseThrow(() -> new AuctionProductNotFoundException("존재하지 않는 경매 상품입니다."));
		if (!auction.getProduct().getMemberId().equals(memberId)) {
			throw new AuctionAccessDeniedException("본인이 등록한 경매만 접근할 수 있습니다.");
		}
		return auction;
	}

	private Map<Long, String> loadCategoryNamesFromAuctions(List<Auction> auctions) {
		List<Long> categoryIds = auctions.stream()
			.map(auction -> auction.getProduct().getCategoryId())
			.distinct()
			.toList();

		Map<Long, String> categoryNamesById = new LinkedHashMap<>();
		categoryRepository.findAllById(categoryIds)
			.forEach(category -> categoryNamesById.put(category.getId(), category.getNameKo()));
		return categoryNamesById;
	}

	private String resolveThumbnailUrl(Product product) {
		return product.getImages().stream()
			.filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
			.sorted(this::compareImagesBySortOrder)
			.map(image -> imageUrlService.toPublicUrl(image.getImageUrl()))
			.findFirst()
			.orElse(null);
	}

	private int compareImagesBySortOrder(ProductImage left, ProductImage right) {
		int leftOrder = left.getSortOrder() == null ? Integer.MAX_VALUE : left.getSortOrder();
		int rightOrder = right.getSortOrder() == null ? Integer.MAX_VALUE : right.getSortOrder();
		return Integer.compare(leftOrder, rightOrder);
	}

	private BigDecimal resolveCurrentPrice(Auction auction) {
		return auction.getCurrentPrice();
	}

	private BigDecimal resolveMinimumNextBidPrice(Auction auction) {
		return resolveCurrentPrice(auction);
	}

	private int getBidCount(Auction auction) {
		return 0;
	}

	private int getBidderCount(Auction auction) {
		return 0;
	}

	private long resolveAuctionDurationDays(Auction auction) {
		if (auction.getAuctionStartAt() == null || auction.getAuctionEndAt() == null) {
			return 0;
		}
		return Duration.between(auction.getAuctionStartAt(), auction.getAuctionEndAt()).toDays();
	}

	private OffsetDateTime toSeoulOffsetDateTime(LocalDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return dateTime.atZone(SEOUL_ZONE).toOffsetDateTime();
	}

	private OffsetDateTime toSeoulOffsetDateTime(OffsetDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return dateTime.atZoneSameInstant(SEOUL_ZONE).toOffsetDateTime();
	}

	@Transactional
	public DeleteAuctionImageResponse deleteImage(Long memberId, Long imageId) {
		loadActiveMember(memberId);
		ProductImage image = productImageRepository.findByImageIdAndDeletedAtIsNull(imageId)
			.orElseThrow(() -> new AuctionImageNotFoundException("존재하지 않는 이미지입니다."));

		if (!image.getMemberId().equals(memberId)) {
			throw new AuctionAccessDeniedException("본인이 업로드한 이미지만 삭제할 수 있습니다.");
		}
		if (image.getProduct() != null) {
			Auction auction = auctionRepository
				.findByProductProductIdAndDeletedAtIsNullAndProductDeletedAtIsNull(image.getProduct().getProductId())
				.orElseThrow(() -> new InvalidAuctionRequestException("경매 상품 이미지가 아닙니다."));
			if (getBidCount(auction) > 0) {
				throw new AuctionConflictException("입찰자가 있는 경매는 수정할 수 없습니다.");
			}
			auction.getProduct().removeImage(image);
		}

		auctionImageStorage.delete(image.getImageUrl());
		image.softDelete();
		return new DeleteAuctionImageResponse(image.getImageId(), true);
	}

	@Transactional
	public SaveAuctionDraftResponse saveDraft(Long memberId, SaveAuctionDraftRequest request) {
		validateDraftRequest(request);
		Member member = loadActiveMember(memberId);

		OffsetDateTime savedAt = OffsetDateTime.now(ZoneOffset.UTC);
		String payloadJson = serializeDraft(request);

		AuctionDraft draft = request.draftId() == null
			? AuctionDraft.create(
				normalizeBlank(request.name()),
				normalizeBlank(request.description()),
				request.saleType(),
				request.categoryId(),
				member.getMemberId(),
				request.price(),
				request.startPrice(),
				null,
				null,
				request.auctionDurationDays(),
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
							member.getMemberId(),
							request.price(),
							request.startPrice(),
							null,
							null,
							request.auctionDurationDays(),
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
						member.getMemberId(),
						request.price(),
						request.startPrice(),
						null,
						null,
						request.auctionDurationDays(),
						normalizeBlank(request.location()),
						payloadJson,
						savedAt
					));

		AuctionDraft savedDraft = auctionDraftRepository.save(draft);
		return new SaveAuctionDraftResponse(savedDraft.getDraftId(), savedDraft.getSavedAt());
	}

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

	public List<CategoryOptionResponse> getCategories() {
		List<Category> categories = categoryRepository.findAllByOrderByDepthAscIdAsc();
		Map<Long, CategoryNode> nodesById = new LinkedHashMap<>();

		for (Category category : categories) {
			nodesById.put(category.getId(), new CategoryNode(category));
		}

		List<CategoryNode> roots = new ArrayList<>();
		for (Category category : categories) {
			CategoryNode node = nodesById.get(category.getId());
			if (category.getParentId() == null) {
				roots.add(node);
				continue;
			}

			CategoryNode parent = nodesById.get(category.getParentId());
			if (parent != null) {
				parent.children().add(node);
			}
		}

		return roots.stream()
			.map(this::toCategoryResponse)
			.toList();
	}

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
	public CreateAuctionResponse createAuction(Long memberId, CreateAuctionRequest request) {
		validateRequestBySaleType(request);
		validateCategorySelection(request.categoryId());
		Member member = loadVerifiedMember(memberId);
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		AuctionPeriod auctionPeriod = resolveAuctionPeriod(request, now);

		Product product = productRepository.save(
			Product.create(
				request.name().trim(),
				request.description().trim(),
				com.dealit.dealit.domain.product.ProductSaleType.AUCTION,
				request.categoryId(),
				member.getMemberId(),
				request.startPrice(),
				false,
				request.location().trim(),
				request.draftId(),
				ProductStatus.ON_SALE
			)
		);

		Auction auction = auctionRepository.save(
			Auction.create(
				product,
				request.startPrice(),
				auctionPeriod.startAt(),
				auctionPeriod.endAt(),
				determineStatus(
					auctionPeriod.endAt(),
					now
				)
			)
		);

		Map<Long, ProductImage> imagesById = loadRequestedProductImages(member.getMemberId(), request.images());
		for (ProductImagePayload imagePayload : request.images()) {
			ProductImage image = imagesById.get(imagePayload.imageId());
			product.attachImage(image, imagePayload.sortOrder());
		}
		productImageRepository.saveAll(imagesById.values());

		return new CreateAuctionResponse(
			product.getProductId(),
			request.saleType(),
			auction.getStatus(),
			buildAuctionScheduleResponse(auction)
		);
	}

	private Map<Long, ProductImage> loadRequestedProductImages(Long memberId, List<ProductImagePayload> imagePayloads) {
		List<Long> imageIds = imagePayloads.stream()
			.map(ProductImagePayload::imageId)
			.toList();

		List<ProductImage> images = productImageRepository.findAllByImageIdInAndDeletedAtIsNull(imageIds);
		if (images.size() != imageIds.size()) {
			throw new AuctionImageNotFoundException("존재하지 않는 이미지가 포함되어 있습니다.");
		}

		Map<Long, ProductImage> imagesById = new LinkedHashMap<>();
		for (ProductImage image : images) {
			if (!image.getMemberId().equals(memberId)) {
				throw new AuctionAccessDeniedException("본인이 업로드한 이미지만 사용할 수 있습니다.");
			}
			if (image.getProduct() != null) {
				throw new InvalidAuctionRequestException("이미 다른 상품에 연결된 이미지가 포함되어 있습니다.");
			}
			imagesById.put(image.getImageId(), image);
		}

		validateDuplicateImageIds(imageIds, imagesById.keySet());
		return imagesById;
	}

	private Map<Long, ProductImage> loadRequestedProductImagesForUpdate(
		Product product,
		List<ProductImagePayload> imagePayloads
	) {
		List<Long> imageIds = imagePayloads.stream()
			.map(ProductImagePayload::imageId)
			.toList();

		List<ProductImage> images = productImageRepository.findAllByImageIdInAndDeletedAtIsNull(imageIds);
		if (images.size() != imageIds.size()) {
			throw new AuctionImageNotFoundException("존재하지 않는 이미지가 포함되어 있습니다.");
		}

		Map<Long, ProductImage> imagesById = new LinkedHashMap<>();
		for (ProductImage image : images) {
			if (!image.getMemberId().equals(product.getMemberId())) {
				throw new AuctionAccessDeniedException("본인이 업로드한 이미지만 사용할 수 있습니다.");
			}
			if (image.getProduct() != null && !image.getProduct().getProductId().equals(product.getProductId())) {
				throw new InvalidAuctionRequestException("이미 다른 상품에 연결된 이미지가 포함되어 있습니다.");
			}
			imagesById.put(image.getImageId(), image);
		}

		validateDuplicateImageIds(imageIds, imagesById.keySet());
		return imagesById;
	}

	private CreateAuctionResponse.AuctionScheduleResponse buildAuctionScheduleResponse(Auction auction) {
		if (auction.getAuctionStartAt() == null || auction.getAuctionEndAt() == null) {
			throw new InvalidAuctionRequestException("경매 응답에는 시작 시각과 종료 시각이 반드시 포함되어야 합니다.");
		}

		return new CreateAuctionResponse.AuctionScheduleResponse(
			auction.getStatus(),
			auction.getAuctionStartAt(),
			auction.getAuctionEndAt()
		);
	}

	private void validateDuplicateImageIds(List<Long> imageIds, Collection<Long> storedImageIds) {
		if (storedImageIds.size() != imageIds.size()) {
			throw new InvalidAuctionRequestException("중복된 이미지 ID는 허용되지 않습니다.");
		}
	}

	private void validateCategorySelection(Long categoryId) {
		Category category = categoryRepository.findById(categoryId)
			.orElseThrow(() -> new InvalidAuctionRequestException("존재하지 않는 카테고리입니다."));

		if (category.getDepth() == null || category.getDepth() != 3) {
			throw new InvalidAuctionRequestException("최하위 카테고리만 선택할 수 있습니다.");
		}
	}

	private Member loadActiveMember(Long memberId) {
		return memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new InvalidCredentialsException("존재하지 않는 회원입니다."));
	}

	private Member loadVerifiedMember(Long memberId) {
		Member member = loadActiveMember(memberId);
		if (!member.isVerified()) {
			throw new EmailNotVerifiedException();
		}
		return member;
	}

	private void validateRequestBySaleType(CreateAuctionRequest request) {
		if (request.saleType() != ProductSaleType.AUCTION) {
			throw new InvalidAuctionRequestException("경매 등록에서는 AUCTION 판매 유형만 허용됩니다.");
		}

		if (request.startPrice() == null) {
			throw new InvalidAuctionRequestException("경매 판매에서는 시작가가 필수입니다.");
		}
		if (request.auctionDurationDays() == null) {
			throw new InvalidAuctionRequestException("경매 판매에서는 진행 기간이 필수입니다.");
		}
		if (request.startPrice().signum() <= 0) {
			throw new InvalidAuctionRequestException("시작가는 0보다 커야 합니다.");
		}
		if (request.auctionDurationDays() <= 0) {
			throw new InvalidAuctionRequestException("경매 진행 기간은 0보다 커야 합니다.");
		}
	}

	private void validateUpdateAuctionRequest(UpdateAuctionRequest request) {
		if (request.startPrice().signum() <= 0) {
			throw new InvalidAuctionRequestException("시작가는 0보다 커야 합니다.");
		}
		if (request.auctionDurationDays() <= 0) {
			throw new InvalidAuctionRequestException("경매 진행 기간은 0보다 커야 합니다.");
		}
	}

	private AuctionPeriod resolveAuctionPeriod(CreateAuctionRequest request, OffsetDateTime now) {
		OffsetDateTime startAt = now;
		OffsetDateTime endAt = startAt.plusDays(request.auctionDurationDays());
		return new AuctionPeriod(startAt, endAt);
	}

	private AuctionStatus determineStatus(
		OffsetDateTime auctionEndAt,
		OffsetDateTime now
	) {
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
		boolean hasAuctionDurationDays = request.auctionDurationDays() != null;
		boolean hasImages = request.images() != null && !request.images().isEmpty();
		boolean hasLocation = normalizeBlank(request.location()) != null;

		if (
			!hasSaleType &&
			!hasName &&
			!hasDescription &&
			!hasCategory &&
			!hasPrice &&
			!hasStartPrice &&
			!hasAuctionDurationDays &&
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

	private CategoryOptionResponse toCategoryResponse(CategoryNode node) {
		Category category = node.category();
		return new CategoryOptionResponse(
			category.getId(),
			category.getNameKo(),
			category.getNameEn(),
			category.getDepth(),
			category.getParentId(),
			node.children().stream()
				.map(this::toCategoryResponse)
				.toList()
		);
	}

	private record CategoryNode(
		Category category,
		List<CategoryNode> children
	) {
		private CategoryNode(Category category) {
			this(category, new ArrayList<>());
		}
	}

	private record AuctionPeriod(
		OffsetDateTime startAt,
		OffsetDateTime endAt
	) {
	}
}
