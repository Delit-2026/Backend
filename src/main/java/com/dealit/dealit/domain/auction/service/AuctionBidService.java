package com.dealit.dealit.domain.auction.service;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.dto.AuctionDetailResponse;
import com.dealit.dealit.domain.auction.dto.AuctionDetailResponse.ImageResponse;
import com.dealit.dealit.domain.auction.dto.AuctionDetailResponse.SellerResponse;
import com.dealit.dealit.domain.auction.dto.AuctionBidHistoryResponse;
import com.dealit.dealit.domain.auction.dto.AuctionBidHistoryResponse.BidHistoryItem;
import com.dealit.dealit.domain.auction.dto.BidResponse;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auction.entity.Bid;
import com.dealit.dealit.domain.auction.event.AuctionEventPublisher;
import com.dealit.dealit.domain.auction.exception.AuctionNotFoundException;
import com.dealit.dealit.domain.auction.exception.InvalidAuctionRequestException;
import com.dealit.dealit.domain.auction.redis.AuctionRedisService;
import com.dealit.dealit.domain.auction.redis.AuctionRedisService.AuctionState;
import com.dealit.dealit.domain.auction.redis.AuctionRedisService.BidScriptResult;
import com.dealit.dealit.domain.auction.repository.AuctionRepository;
import com.dealit.dealit.domain.auction.repository.BidRepository;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.global.service.ImageUrlService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionBidService {

	private final AuctionRepository auctionRepository;
	private final BidRepository bidRepository;
	private final CategoryRepository categoryRepository;
	private final MemberRepository memberRepository;
	private final AuctionRedisService auctionRedisService;
	private final AuctionEventPublisher auctionEventPublisher;
	private final ImageUrlService imageUrlService;
	private final Clock clock;

	public AuctionDetailResponse getAuction(Long auctionId) {
		Auction auction = loadAuctionDetail(auctionId);
		Product product = auction.getProduct();
		BigDecimal currentPrice = auction.getCurrentPrice();
		if (auction.isOngoing()) {
			try {
				AuctionState state = auctionRedisService.getState(auctionId);
				if (state.currentPrice() != null) {
					currentPrice = state.currentPrice();
				}
			} catch (RedisConnectionFailureException exception) {
				currentPrice = auction.getCurrentPrice();
			}
		}
		currentPrice = resolveDisplayCurrentPrice(auction, currentPrice);
		Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
		Member seller = memberRepository.findByMemberIdAndDeletedAtIsNull(product.getMemberId()).orElse(null);
		return new AuctionDetailResponse(
			auction.getAuctionId(),
			product.getProductId(),
			product.getName(),
			product.getDescription(),
			product.getCategoryId(),
			category == null ? "" : category.getNameKo(),
			product.getLocation(),
			toImageResponses(product),
			toSellerResponse(seller),
			auction.getStartPrice(),
			currentPrice,
			auction.getMinimumBidAmount(),
			currentPrice.add(auction.getMinimumBidAmount()),
			(int) bidRepository.countByAuctionAuctionId(auction.getAuctionId()),
			(int) bidRepository.countDistinctBidderIdByAuctionId(auction.getAuctionId()),
			auction.getAuctionStartAt(),
			auction.getEndsAt(),
			serverTime(),
			auction.getStatus()
		);
	}

	public AuctionBidHistoryResponse getBidHistory(Long auctionId) {
		Auction auction = loadAuction(auctionId);
		List<Bid> bids = bidRepository.findAllByAuctionAuctionIdOrderByCreatedAtDescBidIdDesc(auctionId);
		Map<Long, Member> membersById = memberRepository.findAllByMemberIdInAndDeletedAtIsNull(
				bids.stream().map(Bid::getBidderId).collect(Collectors.toSet())
			)
			.stream()
			.collect(Collectors.toMap(Member::getMemberId, Function.identity()));

		List<BidHistoryItem> bidItems = bids.stream()
			.map(bid -> new BidHistoryItem(
				bid.getBidId(),
				bid.getBidderId(),
				resolveBidderNickname(membersById.get(bid.getBidderId()), bid.getBidderId()),
				resolveBidderProfileImageUrl(membersById.get(bid.getBidderId())),
				bid.getBidPrice(),
				bid.getCreatedAt(),
				auction.getWinnerId() == null
					? bid.getBidPrice().compareTo(auction.getCurrentPrice()) == 0
					: bid.getBidderId().equals(auction.getWinnerId())
			))
			.toList();

		return new AuctionBidHistoryResponse(
			auctionId,
			resolveDisplayCurrentPrice(auction, auction.getCurrentPrice()),
			bidItems.size(),
			bidItems
		);
	}

	@Transactional
	public BidResponse bid(Long auctionId, Long bidderId, BigDecimal bidPrice) {
		Auction auction = loadAuction(auctionId);
		if (auction.getProduct().getMemberId().equals(bidderId)) {
			throw new InvalidAuctionRequestException("자신이 등록한 경매에는 입찰할 수 없습니다.");
		}
		if (!auction.isOngoing()) {
			throw new InvalidAuctionRequestException("이미 종료된 경매입니다.");
		}
		if (!auction.getEndsAt().isAfter(serverTime())) {
			throw new InvalidAuctionRequestException("이미 종료된 경매입니다.");
		}
		if (bidPrice.compareTo(auction.getCurrentPrice().add(auction.getMinimumBidAmount())) < 0) {
			throw new InvalidAuctionRequestException("최소 입찰 금액을 충족해야 합니다.");
		}

		BidScriptResult result = auctionRedisService.bid(auctionId, bidPrice, bidderId);
		switch (result.status()) {
			case AUCTION_ENDED -> throw new InvalidAuctionRequestException("이미 종료된 경매입니다.");
			case BID_TOO_LOW -> throw new InvalidAuctionRequestException("현재가보다 높은 금액만 입찰할 수 있습니다.");
			case SUCCESS -> {
				bidRepository.save(Bid.create(auction, bidderId, bidPrice));
				auction.updateCurrentPrice(bidPrice);
				OffsetDateTime serverTime = serverTime();
				long bidCount = bidRepository.countByAuctionAuctionId(auction.getAuctionId());
				long bidderCount = bidRepository.countDistinctBidderIdByAuctionId(auction.getAuctionId());
				BigDecimal minimumNextBidPrice = bidPrice.add(auction.getMinimumBidAmount());
				Long sellerId = auction.getProduct().getMemberId();
				auctionEventPublisher.publishBidUpdated(auctionId, bidPrice, bidderId, serverTime);
				auctionEventPublisher.publishOutbid(auctionId, result.previousBidderId(), bidderId, bidPrice, serverTime);
				auctionEventPublisher.publishBidReceived(
					sellerId,
					auctionId,
					bidderId,
					bidPrice,
					bidCount,
					result.previousBidderId() == null,
					serverTime
				);
				auctionEventPublisher.publishAuctionBidUpdated(
					Arrays.asList(sellerId, bidderId, result.previousBidderId()),
					auctionId,
					bidPrice,
					minimumNextBidPrice,
					bidCount,
					bidderCount,
					serverTime
				);
				return new BidResponse(auctionId, bidPrice, bidderId, serverTime);
			}
		}
		throw new InvalidAuctionRequestException("입찰을 처리할 수 없습니다.");
	}

	@Transactional
	public void endAuction(Long auctionId) {
		Auction auction = loadAuction(auctionId);
		if (!auction.isOngoing()) {
			return;
		}

		AuctionState state = auctionRedisService.getState(auctionId);
		if (state.highestBidderId() == null) {
			auction.completeWithoutBid();
			auction.getProduct().markEnded();
			auction.softDelete();
			auctionRedisService.removeEnding(auctionId);
			auctionRedisService.deleteState(auctionId);
			auctionEventPublisher.publishAuctionEnded(
				auction.getProduct().getMemberId(),
				auctionId,
				null,
				null,
				AuctionStatus.NO_BID,
				serverTime()
			);
			return;
		}

		auction.completeWithWinner(state.highestBidderId(), state.currentPrice());
		auction.getProduct().markSold();
		auctionRedisService.removeEnding(auctionId);
		auctionRedisService.deleteState(auctionId);
		auctionEventPublisher.publishAuctionEnded(
			auction.getProduct().getMemberId(),
			auctionId,
			state.highestBidderId(),
			state.currentPrice(),
			AuctionStatus.SUCCESSFUL_BID,
			serverTime()
		);
		if (!auction.getProduct().getMemberId().equals(state.highestBidderId())) {
			auctionEventPublisher.publishAuctionEnded(
				state.highestBidderId(),
				auctionId,
				state.highestBidderId(),
				state.currentPrice(),
				AuctionStatus.SUCCESSFUL_BID,
				serverTime()
			);
		}
	}

	private Auction loadAuction(Long auctionId) {
		return auctionRepository.findByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(auctionId)
			.orElseThrow(() -> new AuctionNotFoundException("존재하지 않는 경매입니다."));
	}

	private Auction loadAuctionDetail(Long auctionId) {
		return auctionRepository.findDetailByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(auctionId)
			.orElseThrow(() -> new AuctionNotFoundException("존재하지 않는 경매입니다."));
	}

	private List<ImageResponse> toImageResponses(Product product) {
		return product.getImages().stream()
			.filter(image -> image.getDeletedAt() == null)
			.filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
			.sorted(Comparator.comparing(
				ProductImage::getSortOrder,
				Comparator.nullsLast(Integer::compareTo)
			))
			.map(image -> new ImageResponse(
				image.getImageId(),
				imageUrlService.toPublicUrl(image.getImageUrl()),
				image.getSortOrder()
			))
			.toList();
	}

	private SellerResponse toSellerResponse(Member seller) {
		if (seller == null) {
			return null;
		}
		String profileImageUrl = seller.getProfileImage() == null || seller.getProfileImage().isBlank()
			? null
			: imageUrlService.toPublicUrl(seller.getProfileImage());
		return new SellerResponse(seller.getMemberId(), seller.getNickname(), profileImageUrl);
	}

	private String resolveBidderNickname(Member bidder, Long bidderId) {
		if (bidder == null) {
			return "입찰자 #" + bidderId;
		}
		return bidder.getNickname();
	}

	private String resolveBidderProfileImageUrl(Member bidder) {
		if (bidder == null || bidder.getProfileImage() == null || bidder.getProfileImage().isBlank()) {
			return null;
		}
		return imageUrlService.toPublicUrl(bidder.getProfileImage());
	}

	private BigDecimal resolveDisplayCurrentPrice(Auction auction, BigDecimal currentPrice) {
		if (currentPrice == null || currentPrice.signum() <= 0) {
			return auction.getStartPrice();
		}
		return currentPrice;
	}

	private OffsetDateTime serverTime() {
		return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
	}
}
