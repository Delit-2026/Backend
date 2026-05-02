package com.dealit.dealit.domain.wishlist.service;

import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.exception.InvalidProductRequestException;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.wishlist.dto.MyWishlistItemResponse;
import com.dealit.dealit.domain.wishlist.dto.MyWishlistListResponse;
import com.dealit.dealit.domain.wishlist.dto.WishlistToggleResponse;
import com.dealit.dealit.domain.wishlist.entity.Wishlist;
import com.dealit.dealit.domain.wishlist.repository.WishlistRepository;
import com.dealit.dealit.global.service.ImageUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private final WishlistRepository wishlistRepository;
	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;
	private final CategoryRepository categoryRepository;
	private final ImageUrlService imageUrlService;

	@Transactional
	public WishlistToggleResponse addWishlist(Long memberId, Long productId) {
		loadActiveMember(memberId);
		Product product = loadProduct(productId);
		validateWishlistTarget(memberId, product);

		Wishlist wishlist = wishlistRepository.findByMemberIdAndProductProductId(memberId, productId)
			.map(existing -> {
				if (existing.getDeletedAt() == null) {
					throw new InvalidProductRequestException("이미 찜한 상품입니다.");
				}
				existing.restore();
				return existing;
			})
			.orElseGet(() -> Wishlist.create(memberId, product));

		wishlistRepository.save(wishlist);
		product.increaseFavoriteCount();

		return new WishlistToggleResponse(product.getProductId(), true, product.getFavoriteCount());
	}

	@Transactional
	public WishlistToggleResponse removeWishlist(Long memberId, Long productId) {
		loadActiveMember(memberId);
		Product product = loadProduct(productId);
		Wishlist wishlist = wishlistRepository.findByMemberIdAndProductProductIdAndDeletedAtIsNull(memberId, productId)
			.orElseThrow(() -> new InvalidProductRequestException("찜한 상품이 아닙니다."));

		wishlist.softDelete();
		product.decreaseFavoriteCount();
		return new WishlistToggleResponse(product.getProductId(), false, product.getFavoriteCount());
	}

	public MyWishlistListResponse getMyWishlist(Long memberId, int page, int size) {
		loadActiveMember(memberId);
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = Math.min(Math.max(size, 1), 100);

		Page<Wishlist> wishlistPage = wishlistRepository.findAllByMemberIdAndDeletedAtIsNull(
			memberId,
			PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
		);

		Map<Long, String> categoryNamesById = loadCategoryNames(wishlistPage.getContent());
		List<MyWishlistItemResponse> content = wishlistPage.getContent().stream()
			.map(wishlist -> toMyWishlistItemResponse(wishlist, categoryNamesById))
			.toList();

		return new MyWishlistListResponse(
			content,
			wishlistPage.getNumber(),
			wishlistPage.getSize(),
			wishlistPage.getTotalElements(),
			wishlistPage.hasNext()
		);
	}

	public long countWishlist(Long memberId) {
		return wishlistRepository.countByMemberIdAndDeletedAtIsNull(memberId);
	}

	public boolean isLiked(Long memberId, Long productId) {
		return wishlistRepository.existsByMemberIdAndProductProductIdAndDeletedAtIsNull(memberId, productId);
	}

	private Member loadActiveMember(Long memberId) {
		return memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new InvalidCredentialsException("존재하지 않는 회원입니다."));
	}

	private Product loadProduct(Long productId) {
		return productRepository.findByProductIdAndDeletedAtIsNull(productId)
			.orElseThrow(() -> new InvalidProductRequestException("존재하지 않는 상품입니다."));
	}

	private void validateWishlistTarget(Long memberId, Product product) {
		if (product.getDeletedAt() != null) {
			throw new InvalidProductRequestException("삭제된 상품은 찜할 수 없습니다.");
		}
		if (product.getMemberId().equals(memberId)) {
			throw new InvalidProductRequestException("본인 상품은 찜할 수 없습니다.");
		}
	}

	private Map<Long, String> loadCategoryNames(List<Wishlist> wishlists) {
		Set<Long> categoryIds = wishlists.stream()
			.map(wishlist -> wishlist.getProduct().getCategoryId())
			.collect(java.util.stream.Collectors.toSet());

		Map<Long, String> categoryNamesById = new HashMap<>();
		categoryRepository.findAllById(categoryIds)
			.forEach(category -> categoryNamesById.put(category.getId(), category.getNameKo()));
		return categoryNamesById;
	}

	private MyWishlistItemResponse toMyWishlistItemResponse(Wishlist wishlist, Map<Long, String> categoryNamesById) {
		Product product = wishlist.getProduct();
		return new MyWishlistItemResponse(
			product.getProductId(),
			product.getName(),
			product.getSaleType(),
			product.getStatus(),
			resolveThumbnailUrl(product),
			product.getPrice(),
			categoryNamesById.getOrDefault(product.getCategoryId(), ""),
			product.getLocation(),
			product.getFavoriteCount(),
			toSeoulOffsetDateTime(wishlist.getCreatedAt())
		);
	}

	private String resolveThumbnailUrl(Product product) {
		return product.getImages().stream()
			.filter(image -> image.getDeletedAt() == null)
			.filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
			.sorted(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
				.thenComparing(ProductImage::getCreatedAt))
			.map(image -> imageUrlService.toPublicUrl(image.getImageUrl()))
			.findFirst()
			.orElse(null);
	}

	private OffsetDateTime toSeoulOffsetDateTime(LocalDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return dateTime.atZone(SEOUL_ZONE).toOffsetDateTime();
	}
}
