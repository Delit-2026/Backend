package com.dealit.dealit.domain.recentproduct.service;

import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auction.repository.AuctionRepository;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.product.service.ProductThumbnailResolver;
import com.dealit.dealit.domain.recentproduct.RecentProductType;
import com.dealit.dealit.domain.recentproduct.dto.RecentProductItemResponse;
import com.dealit.dealit.domain.recentproduct.dto.RecentProductListResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentProductService {

	private static final String KEY_PREFIX = "recent-products:";
	private static final String ALL = "ALL";
	private static final int MAX_RECENT_PRODUCTS = 100;

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private final StringRedisTemplate stringRedisTemplate;
	private final ProductRepository productRepository;
	private final AuctionRepository auctionRepository;
	private final CategoryRepository categoryRepository;
	private final ProductThumbnailResolver productThumbnailResolver;
	private final Clock clock;

	public void recordRegularProduct(Long memberId, Long productId) {
		record(memberId, RecentProductType.REGULAR, productId);
	}

	public void recordAuction(Long memberId, Long auctionId) {
		record(memberId, RecentProductType.AUCTION, auctionId);
	}

	public RecentProductListResponse findRecentProducts(Long memberId, int size) {
		int normalizedSize = Math.min(Math.max(size, 1), 50);
		try {
			Set<TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
				.reverseRangeWithScores(key(memberId), 0, normalizedSize - 1);
			if (tuples == null || tuples.isEmpty()) {
				return new RecentProductListResponse(List.of(), normalizedSize);
			}
			return new RecentProductListResponse(resolveItems(tuples), normalizedSize);
		} catch (RuntimeException exception) {
			return new RecentProductListResponse(List.of(), normalizedSize);
		}
	}

	private void record(Long memberId, RecentProductType type, Long id) {
		if (memberId == null || type == null || id == null) {
			return;
		}
		try {
			String value = value(type, id);
			double score = clock.millis();
			recordToKey(key(memberId), value, score);
		} catch (RuntimeException exception) {
			// Product detail should still work even if Redis recent-view storage is temporarily unavailable.
		}
	}

	private void recordToKey(String key, String value, double score) {
		stringRedisTemplate.opsForZSet().add(key, value, score);
		stringRedisTemplate.opsForZSet().removeRange(key, 0, -(MAX_RECENT_PRODUCTS + 1L));
	}

	private List<RecentProductItemResponse> resolveItems(Collection<TypedTuple<String>> tuples) {
		List<RecentProductRef> refs = tuples.stream()
			.map(this::toRef)
			.filter(Objects::nonNull)
			.toList();
		if (refs.isEmpty()) {
			return List.of();
		}

		Set<Long> productIds = refs.stream()
			.filter(ref -> ref.type() == RecentProductType.REGULAR)
			.map(RecentProductRef::id)
			.collect(Collectors.toSet());
		Set<Long> auctionIds = refs.stream()
			.filter(ref -> ref.type() == RecentProductType.AUCTION)
			.map(RecentProductRef::id)
			.collect(Collectors.toSet());

		Map<Long, Product> productsById = loadProductsById(productIds);
		Map<Long, Auction> auctionsById = loadAuctionsById(auctionIds);

		Map<Long, String> categoryNamesById = loadCategoryNames(productsById.values(), auctionsById.values());
		List<RecentProductItemResponse> responses = new ArrayList<>();
		for (RecentProductRef ref : refs) {
			RecentProductItemResponse response = switch (ref.type()) {
				case REGULAR -> toRegularResponse(productsById.get(ref.id()), categoryNamesById, ref.viewedAt());
				case AUCTION -> toAuctionResponse(auctionsById.get(ref.id()), categoryNamesById, ref.viewedAt());
			};
			if (response != null) {
				responses.add(response);
			}
		}
		return responses;
	}

	private Map<Long, Product> loadProductsById(Set<Long> productIds) {
		if (productIds.isEmpty()) {
			return Map.of();
		}
		return productRepository.findAllByProductIdInAndDeletedAtIsNull(productIds)
			.stream()
			.collect(Collectors.toMap(Product::getProductId, Function.identity()));
	}

	private Map<Long, Auction> loadAuctionsById(Set<Long> auctionIds) {
		if (auctionIds.isEmpty()) {
			return Map.of();
		}
		return auctionRepository.findAllByAuctionIdInAndDeletedAtIsNullAndProductDeletedAtIsNull(auctionIds)
			.stream()
			.collect(Collectors.toMap(Auction::getAuctionId, Function.identity()));
	}

	private Map<Long, String> loadCategoryNames(Collection<Product> products, Collection<Auction> auctions) {
		Set<Long> categoryIds = new java.util.HashSet<>();
		products.stream()
			.map(Product::getCategoryId)
			.filter(Objects::nonNull)
			.forEach(categoryIds::add);
		auctions.stream()
			.map(Auction::getProduct)
			.map(Product::getCategoryId)
			.filter(Objects::nonNull)
			.forEach(categoryIds::add);
		if (categoryIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> categoryNamesById = new LinkedHashMap<>();
		categoryRepository.findAllById(categoryIds)
			.forEach(category -> categoryNamesById.put(category.getId(), categoryName(category)));
		return categoryNamesById;
	}

	private RecentProductItemResponse toRegularResponse(Product product, Map<Long, String> categoryNamesById, OffsetDateTime viewedAt) {
		if (product == null) {
			return null;
		}
		return new RecentProductItemResponse(
			RecentProductType.REGULAR,
			product.getProductId(),
			null,
			product.getName(),
			productThumbnailResolver.resolve(product),
			product.getPrice(),
			null,
			product.getLocation(),
			categoryNamesById.getOrDefault(product.getCategoryId(), ""),
			viewedAt
		);
	}

	private RecentProductItemResponse toAuctionResponse(Auction auction, Map<Long, String> categoryNamesById, OffsetDateTime viewedAt) {
		if (auction == null) {
			return null;
		}
		Product product = auction.getProduct();
		return new RecentProductItemResponse(
			RecentProductType.AUCTION,
			product.getProductId(),
			auction.getAuctionId(),
			product.getName(),
			productThumbnailResolver.resolve(product),
			null,
			auction.resolveDisplayCurrentPrice(auction.getCurrentPrice()),
			product.getLocation(),
			categoryNamesById.getOrDefault(product.getCategoryId(), ""),
			viewedAt
		);
	}

	private String categoryName(Category category) {
		return category == null ? "" : category.getNameKo();
	}

	private RecentProductRef toRef(TypedTuple<String> tuple) {
		if (tuple == null || tuple.getValue() == null || tuple.getScore() == null) {
			return null;
		}
		String[] parts = tuple.getValue().split(":", 2);
		if (parts.length != 2) {
			return null;
		}
		try {
			RecentProductType type = RecentProductType.valueOf(parts[0]);
			Long id = Long.valueOf(parts[1]);
			OffsetDateTime viewedAt = Instant.ofEpochMilli(tuple.getScore().longValue())
				.atZone(SEOUL_ZONE)
				.toOffsetDateTime();
			return new RecentProductRef(type, id, viewedAt);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private String key(Long memberId) {
		return KEY_PREFIX + memberId + ":" + ALL;
	}

	private String value(RecentProductType type, Long id) {
		return type.name() + ":" + id;
	}

	private record RecentProductRef(
		RecentProductType type,
		Long id,
		OffsetDateTime viewedAt
	) {
	}
}
