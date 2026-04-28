package com.dealit.dealit.domain.product.service;

import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.dto.CategoryOptionResponse;
import com.dealit.dealit.domain.product.dto.CreateProductRequest;
import com.dealit.dealit.domain.product.dto.CreateProductResponse;
import com.dealit.dealit.domain.product.dto.DeleteProductImageResponse;
import com.dealit.dealit.domain.product.dto.ProductImagePayload;
import com.dealit.dealit.domain.product.dto.RecommendCategoryRequest;
import com.dealit.dealit.domain.product.dto.RecommendCategoryResponse;
import com.dealit.dealit.domain.product.dto.RecommendPriceRequest;
import com.dealit.dealit.domain.product.dto.RecommendPriceResponse;
import com.dealit.dealit.domain.product.dto.SaveProductDraftRequest;
import com.dealit.dealit.domain.product.dto.SaveProductDraftResponse;
import com.dealit.dealit.domain.product.dto.UploadProductImageResponse;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductDraft;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.exception.ProductAccessDeniedException;
import com.dealit.dealit.domain.product.exception.InvalidProductRequestException;
import com.dealit.dealit.domain.product.exception.ProductImageNotFoundException;
import com.dealit.dealit.domain.product.repository.ProductDraftRepository;
import com.dealit.dealit.domain.product.repository.ProductImageRepository;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.global.service.ImageUrlService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final ProductDraftRepository productDraftRepository;
	private final CategoryRepository categoryRepository;
	private final MemberRepository memberRepository;
	private final ProductImageStorage productImageStorage;
	private final ImageUrlService imageUrlService;
	private final ObjectMapper objectMapper;

	@Transactional
	public UploadProductImageResponse uploadImage(Long memberId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidProductRequestException("이미지 파일은 필수입니다.");
		}

		loadActiveMember(memberId);
		String originalFilename = file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename().trim();
		ProductImage savedImage = productImageRepository.save(ProductImage.createTemporary("", originalFilename, memberId));

		String storedFileName = productImageStorage.store(savedImage.getImageId(), file, originalFilename);
		savedImage.updateImageUrl(imageUrlService.toProductImagePath(storedFileName));
		return new UploadProductImageResponse(savedImage.getImageId(), imageUrlService.toPublicUrl(savedImage.getImageUrl()));
	}

	@Transactional
	public DeleteProductImageResponse deleteImage(Long memberId, Long imageId) {
		ProductImage image = productImageRepository.findByImageIdAndDeletedAtIsNull(imageId)
			.orElseThrow(() -> new ProductImageNotFoundException("존재하지 않는 이미지입니다."));

		if (!image.getMemberId().equals(memberId)) {
			throw new ProductAccessDeniedException("본인이 업로드한 이미지만 삭제할 수 있습니다.");
		}

		if (image.getProduct() != null) {
			throw new InvalidProductRequestException("이미 상품에 연결된 이미지는 삭제할 수 없습니다.");
		}

		productImageStorage.delete(image.getImageUrl());
		image.softDelete();
		return new DeleteProductImageResponse(image.getImageId(), true);
	}

	@Transactional
	public SaveProductDraftResponse saveDraft(Long memberId, SaveProductDraftRequest request) {
		validateDraftRequest(request);
		Member member = loadActiveMember(memberId);
		String location = resolveLocation(member, request.location());

		OffsetDateTime savedAt = OffsetDateTime.now(ZoneOffset.UTC);
		String payloadJson = serializeDraft(request);

		ProductDraft draft = request.draftId() == null
			? ProductDraft.create(
				normalizeBlank(request.name()),
				normalizeBlank(request.description()),
				request.saleType(),
				request.categoryId(),
				member.getMemberId(),
				request.price(),
				request.allowOffer(),
				location,
				payloadJson,
				savedAt
			)
			: productDraftRepository.findById(request.draftId())
				.map(existingDraft -> {
					existingDraft.update(
						normalizeBlank(request.name()),
						normalizeBlank(request.description()),
						request.saleType(),
						request.categoryId(),
						member.getMemberId(),
						request.price(),
						request.allowOffer(),
						location,
						payloadJson,
						savedAt
					);
					return existingDraft;
				})
				.orElseGet(() -> ProductDraft.create(
					normalizeBlank(request.name()),
					normalizeBlank(request.description()),
					request.saleType(),
					request.categoryId(),
					member.getMemberId(),
					request.price(),
					request.allowOffer(),
					location,
					payloadJson,
					savedAt
				));

		ProductDraft savedDraft = productDraftRepository.save(draft);
		return new SaveProductDraftResponse(savedDraft.getDraftId(), savedDraft.getSavedAt());
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

		return roots.stream().map(this::toCategoryResponse).toList();
	}

	public RecommendPriceResponse recommendPrice(RecommendPriceRequest request) {
		if (request.saleType() != ProductSaleType.REGULAR) {
			throw new InvalidProductRequestException("일반 상품 가격 추천에서는 판매 유형이 REGULAR여야 합니다.");
		}

		int textWeight = request.name().trim().length() * 10 + request.description().trim().length() * 2;
		BigDecimal recommendedPrice = BigDecimal.valueOf(Math.max(10000, textWeight * 100L));
		return new RecommendPriceResponse(recommendedPrice);
	}

	@Transactional
	public CreateProductResponse createProduct(Long memberId, CreateProductRequest request) {
		validateCreateRequest(request);
		validateCategorySelection(request.categoryId());
		Member member = loadActiveMember(memberId);
		String location = resolveLocation(member, request.location());

		Product product = productRepository.save(
			Product.create(
				request.name().trim(),
				request.description().trim(),
				request.saleType(),
				request.categoryId(),
				member.getMemberId(),
				request.price(),
				request.allowOffer(),
				location,
				request.draftId(),
				ProductStatus.ON_SALE
			)
		);

		Map<Long, ProductImage> imagesById = loadRequestedImages(request.images());
		for (ProductImagePayload imagePayload : request.images()) {
			ProductImage image = imagesById.get(imagePayload.imageId());
			product.attachImage(image, imagePayload.sortOrder());
		}
		productImageRepository.saveAll(imagesById.values());

		return new CreateProductResponse(
			product.getProductId(),
			product.getSaleType(),
			product.getStatus(),
			null,
			new CreateProductResponse.GeneralSaleResponse(product.getPrice())
		);
	}

	private Map<Long, ProductImage> loadRequestedImages(List<ProductImagePayload> imagePayloads) {
		List<Long> imageIds = imagePayloads.stream().map(ProductImagePayload::imageId).toList();

		List<ProductImage> images = productImageRepository.findAllByImageIdInAndDeletedAtIsNull(imageIds);
		if (images.size() != imageIds.size()) {
			throw new ProductImageNotFoundException("존재하지 않는 이미지가 포함되어 있습니다.");
		}

		Map<Long, ProductImage> imagesById = new LinkedHashMap<>();
		for (ProductImage image : images) {
			if (image.getProduct() != null) {
				throw new InvalidProductRequestException("이미 다른 상품에 연결된 이미지가 포함되어 있습니다.");
			}
			imagesById.put(image.getImageId(), image);
		}

		validateDuplicateImageIds(imageIds, imagesById.keySet());
		return imagesById;
	}

	private void validateDuplicateImageIds(List<Long> imageIds, Collection<Long> storedImageIds) {
		if (storedImageIds.size() != imageIds.size()) {
			throw new InvalidProductRequestException("중복된 이미지 ID는 허용되지 않습니다.");
		}
	}

	private void validateCategorySelection(Long categoryId) {
		Category category = categoryRepository.findById(categoryId)
			.orElseThrow(() -> new InvalidProductRequestException("존재하지 않는 카테고리입니다."));

		if (category.getDepth() == null || category.getDepth() != 3) {
			throw new InvalidProductRequestException("최하위 카테고리만 선택할 수 있습니다.");
		}
	}

	private void validateCreateRequest(CreateProductRequest request) {
		if (request.saleType() != ProductSaleType.REGULAR) {
			throw new InvalidProductRequestException("일반 상품 등록에서는 판매 유형이 REGULAR여야 합니다.");
		}
		if (request.price().signum() <= 0) {
			throw new InvalidProductRequestException("판매가는 0보다 커야 합니다.");
		}
	}

	private void validateDraftRequest(SaveProductDraftRequest request) {
		boolean hasSaleType = request.saleType() != null;
		boolean hasName = normalizeBlank(request.name()) != null;
		boolean hasDescription = normalizeBlank(request.description()) != null;
		boolean hasCategory = request.categoryId() != null;
		boolean hasPrice = request.price() != null;
		boolean hasAllowOffer = request.allowOffer() != null;
		boolean hasImages = request.images() != null && !request.images().isEmpty();
		boolean hasLocation = normalizeBlank(request.location()) != null;

		if (!hasSaleType && !hasName && !hasDescription && !hasCategory && !hasPrice && !hasAllowOffer && !hasImages && !hasLocation) {
			throw new InvalidProductRequestException("임시저장할 내용이 없습니다.");
		}
		if (request.saleType() != null && request.saleType() != ProductSaleType.REGULAR) {
			throw new InvalidProductRequestException("일반 상품 임시저장에서는 판매 유형이 REGULAR여야 합니다.");
		}
		if (request.price() != null && request.price().signum() <= 0) {
			throw new InvalidProductRequestException("판매가는 0보다 커야 합니다.");
		}
	}

	private Member loadActiveMember(Long memberId) {
		return memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new InvalidCredentialsException("존재하지 않는 회원입니다."));
	}

	private String resolveLocation(Member member, String requestLocation) {
		String location = normalizeBlank(requestLocation);
		if (location != null) {
			return location;
		}

		String memberLocation = normalizeBlank(member.getLocation());
		if (memberLocation == null) {
			throw new InvalidProductRequestException("거래 위치는 필수입니다.");
		}

		return memberLocation;
	}

	private String serializeDraft(SaveProductDraftRequest request) {
		try {
			return objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException exception) {
			throw new InvalidProductRequestException("임시저장 데이터를 처리하는 중 오류가 발생했습니다.");
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
			node.children().stream().map(this::toCategoryResponse).toList()
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
}
