package com.dealit.dealit.domain.product.service;

import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.dto.ProductDetailResponse;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.exception.ProductNotFoundException;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.recentproduct.service.RecentProductService;
import com.dealit.dealit.domain.review.dto.ReviewRatingSummaryResponse;
import com.dealit.dealit.domain.review.repository.ReviewRepository;
import com.dealit.dealit.global.service.ImageUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDetailService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final MemberRepository memberRepository;
	private final ImageUrlService imageUrlService;
	private final RecentProductService recentProductService;
	private final ReviewRepository reviewRepository;

	@Transactional
	public ProductDetailResponse getProductDetail(Long memberId, Long productId) {
		Member viewer = loadActiveMember(memberId);
		Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
			.orElseThrow(() -> new ProductNotFoundException("존재하지 않는 상품입니다."));

		if (product.getSaleType() != ProductSaleType.REGULAR) {
			throw new ProductNotFoundException("존재하지 않는 상품입니다.");
		}

		product.increaseViewCount();
		recentProductService.recordRegularProduct(viewer.getMemberId(), product.getProductId());

		Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
		Member seller = memberRepository.findByMemberIdAndDeletedAtIsNull(product.getMemberId())
			.orElseThrow(() -> new ProductNotFoundException("상품 판매자 정보를 찾을 수 없습니다."));

		boolean owner = seller.getMemberId().equals(viewer.getMemberId());

		return new ProductDetailResponse(
			product.getProductId(),
			product.getName(),
			product.getDescription(),
			product.getSaleType(),
			toCategoryResponse(category, product.getCategoryId()),
			toImageUrls(product),
			product.getStatus(),
			toSellerResponse(seller, product),
			toSeoulOffsetDateTime(product.getCreatedAt()),
			toSeoulOffsetDateTime(product.getUpdatedAt() == null ? product.getCreatedAt() : product.getUpdatedAt()),
			toGeneralSaleResponse(product),
			canChat(product, owner),
			canPurchase(product, owner, viewer),
			purchaseBlockedReason(product, owner, viewer),
			canFavorite(product, owner)
		);
	}

	private Member loadActiveMember(Long memberId) {
		return memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new InvalidCredentialsException("존재하지 않는 회원입니다."));
	}

	private ProductDetailResponse.CategoryResponse toCategoryResponse(Category category, Long categoryId) {
		if (category == null) {
			return new ProductDetailResponse.CategoryResponse(categoryId, "", "");
		}

		return new ProductDetailResponse.CategoryResponse(
			category.getId(),
			category.getNameKo(),
			category.getNameEn()
		);
	}

	private ProductDetailResponse.SellerResponse toSellerResponse(Member seller, Product product) {
		String profileImageUrl = seller.getProfileImage() == null || seller.getProfileImage().isBlank()
			? null
			: imageUrlService.toPublicUrl(seller.getProfileImage());

		return new ProductDetailResponse.SellerResponse(
			seller.getMemberId(),
			seller.getNickname(),
			profileImageUrl,
			product.getLocation(),
			getSellerRating(seller.getMemberId())
		);
	}

	private double getSellerRating(Long sellerId) {
		ReviewRatingSummaryResponse summary = reviewRepository.getRatingSummaryByRevieweeId(sellerId);
		if (summary == null) {
			return 0.0;
		}
		return Math.round(summary.averageRating() * 10.0) / 10.0;
	}

	private List<String> toImageUrls(Product product) {
		return product.getImages().stream()
			.filter(image -> image.getDeletedAt() == null)
			.filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
			.sorted(Comparator.comparing(
				ProductImage::getSortOrder,
				Comparator.nullsLast(Integer::compareTo)
			))
			.map(image -> imageUrlService.toPublicUrl(image.getImageUrl()))
			.toList();
	}

	private ProductDetailResponse.GeneralSaleResponse toGeneralSaleResponse(Product product) {
		return new ProductDetailResponse.GeneralSaleResponse(
			product.getPrice(),
			product.getViewCount(),
			product.getFavoriteCount(),
			product.getChatCount(),
			product.getStatus()
		);
	}

	private boolean canChat(Product product, boolean owner) {
		return !owner && product.getStatus() == ProductStatus.ON_SALE;
	}

	private boolean canPurchase(Product product, boolean owner, Member viewer) {
		return purchaseBlockedReason(product, owner, viewer) == null;
	}

	private String purchaseBlockedReason(Product product, boolean owner, Member viewer) {
		if (!viewer.isVerified()) {
			return "EMAIL_NOT_VERIFIED";
		}
		if (owner) {
			return "OWN_PRODUCT";
		}
		if (product.getStatus() != ProductStatus.ON_SALE) {
			return "PRODUCT_NOT_PURCHASABLE";
		}
		return null;
	}

	private boolean canFavorite(Product product, boolean owner) {
		return !owner && product.getStatus() == ProductStatus.ON_SALE;
	}

	private OffsetDateTime toSeoulOffsetDateTime(LocalDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}

		return dateTime.atZone(SEOUL_ZONE).toOffsetDateTime();
	}
}
