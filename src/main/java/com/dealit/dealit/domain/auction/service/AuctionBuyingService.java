package com.dealit.dealit.domain.auction.service;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.BuyingAuctionStatus;
import com.dealit.dealit.domain.auction.dto.MyBuyingAuctionItemResponse;
import com.dealit.dealit.domain.auction.dto.MyBuyingAuctionListResponse;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.entity.BuyingAuctionHidden;
import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auction.exception.AuctionConflictException;
import com.dealit.dealit.domain.auction.exception.AuctionNotFoundException;
import com.dealit.dealit.domain.auction.exception.InvalidAuctionRequestException;
import com.dealit.dealit.domain.auction.repository.AuctionRepository;
import com.dealit.dealit.domain.auction.repository.BidRepository;
import com.dealit.dealit.domain.auction.repository.BuyingAuctionHiddenRepository;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import com.dealit.dealit.domain.auction.repository.MyBuyingAuctionProjection;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.global.service.ImageUrlService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionBuyingService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private final BidRepository bidRepository;
	private final AuctionRepository auctionRepository;
	private final CategoryRepository categoryRepository;
	private final BuyingAuctionHiddenRepository buyingAuctionHiddenRepository;
	private final ImageUrlService imageUrlService;
	private final Clock clock;

	public MyBuyingAuctionListResponse getMyBuyingAuctions(Long memberId, int page, int size) {
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = Math.min(Math.max(size, 1), 100);
		OffsetDateTime now = OffsetDateTime.now(clock);

		Page<MyBuyingAuctionProjection> buyingPage = bidRepository.findMyBuyingAuctions(
			memberId,
			now,
			PageRequest.of(normalizedPage, normalizedSize)
		);

		List<Long> auctionIds = buyingPage.getContent().stream()
			.map(MyBuyingAuctionProjection::getAuctionId)
			.toList();
		Map<Long, Auction> auctionsById = loadAuctionsById(auctionIds);
		Map<Long, String> categoryNamesById = loadCategoryNamesFromAuctions(auctionsById.values().stream().toList());

		List<MyBuyingAuctionItemResponse> content = buyingPage.getContent().stream()
			.map(projection -> toMyBuyingAuctionItem(memberId, projection, auctionsById, categoryNamesById, now))
			.toList();

		return new MyBuyingAuctionListResponse(
			content,
			buyingPage.getNumber(),
			buyingPage.getSize(),
			buyingPage.getTotalElements(),
			buyingPage.hasNext()
		);
	}

	@Transactional
	public void hideEndedBuyingAuction(Long memberId, Long auctionId) {
		if (!bidRepository.existsByAuctionAuctionIdAndBidderIdAndDeletedAtIsNull(auctionId, memberId)) {
			throw new AuctionNotFoundException("Auction bid history not found.");
		}

		Auction auction = auctionRepository.findDetailByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(auctionId)
			.orElseThrow(() -> new AuctionNotFoundException("Auction not found."));
		if (!isEnded(auction, OffsetDateTime.now(clock))) {
			throw new AuctionConflictException("Only ended buying auctions can be hidden.");
		}

		if (buyingAuctionHiddenRepository.existsByMemberIdAndAuctionIdAndDeletedAtIsNull(memberId, auctionId)) {
			return;
		}
		buyingAuctionHiddenRepository.save(BuyingAuctionHidden.create(memberId, auctionId));
	}

	private MyBuyingAuctionItemResponse toMyBuyingAuctionItem(
		Long memberId,
		MyBuyingAuctionProjection projection,
		Map<Long, Auction> auctionsById,
		Map<Long, String> categoryNamesById,
		OffsetDateTime now
	) {
		Auction auction = auctionsById.get(projection.getAuctionId());
		if (auction == null) {
			throw new InvalidAuctionRequestException("Auction data is invalid.");
		}

		Product product = auction.getProduct();
		return new MyBuyingAuctionItemResponse(
			product.getProductId(),
			auction.getAuctionId(),
			product.getName(),
			resolveThumbnailUrl(product),
			categoryNamesById.getOrDefault(product.getCategoryId(), ""),
			product.getLocation(),
			projection.getMyBidAmount(),
			resolveCurrentHighestBidAmount(auction, projection),
			resolveBuyingStatus(memberId, auction, projection, now),
			auction.getStatus(),
			(int) bidRepository.countByAuctionAuctionId(auction.getAuctionId()),
			(int) bidRepository.countDistinctBidderIdByAuctionId(auction.getAuctionId()),
			toSeoulOffsetDateTime(auction.getAuctionEndAt()),
			toSeoulOffsetDateTime(projection.getLastBidAt())
		);
	}

	private BuyingAuctionStatus resolveBuyingStatus(
		Long memberId,
		Auction auction,
		MyBuyingAuctionProjection projection,
		OffsetDateTime now
	) {
		if (isEnded(auction, now)) {
			return BuyingAuctionStatus.ENDED;
		}
		if (memberId.equals(projection.getHighestBidderId())) {
			return BuyingAuctionStatus.LEADING;
		}
		return BuyingAuctionStatus.OUTBID;
	}

	private boolean isEnded(Auction auction, OffsetDateTime now) {
		return auction.getStatus() != AuctionStatus.ONGOING || !auction.getAuctionEndAt().isAfter(now);
	}

	private BigDecimal resolveCurrentHighestBidAmount(Auction auction, MyBuyingAuctionProjection projection) {
		if (auction.getCurrentPrice() != null && auction.getCurrentPrice().signum() > 0) {
			return auction.getCurrentPrice();
		}
		if (projection.getHighestBidAmount() != null) {
			return projection.getHighestBidAmount();
		}
		return auction.getStartPrice();
	}

	private Map<Long, Auction> loadAuctionsById(List<Long> auctionIds) {
		Map<Long, Auction> auctionsById = new LinkedHashMap<>();
		if (auctionIds.isEmpty()) {
			return auctionsById;
		}
		auctionRepository.findAllByAuctionIdInAndDeletedAtIsNullAndProductDeletedAtIsNull(auctionIds)
			.forEach(auction -> auctionsById.put(auction.getAuctionId(), auction));
		return auctionsById;
	}

	private Map<Long, String> loadCategoryNamesFromAuctions(List<Auction> auctions) {
		List<Long> categoryIds = auctions.stream()
			.map(auction -> auction.getProduct().getCategoryId())
			.distinct()
			.toList();

		Map<Long, String> categoryNamesById = new LinkedHashMap<>();
		categoryRepository.findAllById(categoryIds)
			.forEach(category -> categoryNamesById.put(category.getId(), resolveCategoryName(category)));
		return categoryNamesById;
	}

	private String resolveCategoryName(Category category) {
		return category.getNameKo() == null ? "" : category.getNameKo();
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
}
