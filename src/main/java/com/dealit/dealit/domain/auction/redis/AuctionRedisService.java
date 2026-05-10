package com.dealit.dealit.domain.auction.redis;

import com.dealit.dealit.domain.auction.entity.Auction;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionRedisService {

	public static final String ENDING_KEY = "auction:ending";
	private static final String CLOSING_SOON_NOTIFIED_KEY = "auction:closing-soon:notified";

	private static final DefaultRedisScript<String> BID_SCRIPT = new DefaultRedisScript<>("""
		local currentPrice = tonumber(redis.call('HGET', KEYS[1], 'currentPrice'))
		local minimumBidAmount = tonumber(redis.call('HGET', KEYS[1], 'minimumBidAmount'))
		local endsAt = tonumber(redis.call('HGET', KEYS[1], 'endsAt'))
		local bidPrice = tonumber(ARGV[1])
		local bidderId = ARGV[2]
		local now = tonumber(ARGV[3])

		if currentPrice == nil or endsAt == nil then
		    return 'AUCTION_ENDED'
		end

		if now >= endsAt then
		    return 'AUCTION_ENDED'
		end

		if minimumBidAmount == nil then
		    minimumBidAmount = 0
		end

		if minimumBidAmount <= 0 and bidPrice <= currentPrice then
		    return 'BID_TOO_LOW'
		end

		if minimumBidAmount > 0 and bidPrice < currentPrice + minimumBidAmount then
		    return 'BID_TOO_LOW'
		end

		local previousBidderId = redis.call('HGET', KEYS[1], 'highestBidderId')
		local previousPrice = currentPrice

		if previousBidderId == bidderId then
		    return 'SAME_BIDDER:' .. previousPrice
		end

		redis.call('HSET', KEYS[1],
		    'currentPrice', bidPrice,
		    'highestBidderId', bidderId
		)

		if previousBidderId == false then
		    previousBidderId = ''
		end

		return 'SUCCESS:' .. previousBidderId .. ':' .. previousPrice
		""", String.class);

	private static final DefaultRedisScript<String> RESTORE_BID_SCRIPT = new DefaultRedisScript<>("""
		local currentPrice = redis.call('HGET', KEYS[1], 'currentPrice')
		local highestBidderId = redis.call('HGET', KEYS[1], 'highestBidderId')
		local failedBidPrice = ARGV[1]
		local failedBidderId = ARGV[2]
		local previousPrice = ARGV[3]
		local previousBidderId = ARGV[4]

		if currentPrice == failedBidPrice and highestBidderId == failedBidderId then
		    redis.call('HSET', KEYS[1],
		        'currentPrice', previousPrice,
		        'highestBidderId', previousBidderId
		    )
		    return 'RESTORED'
		end

		return 'SKIPPED'
		""", String.class);

	private final StringRedisTemplate stringRedisTemplate;
	private final Clock clock;

	public void initialize(Auction auction) {
		String stateKey = stateKey(auction.getAuctionId());
		long endsAtEpochMillis = auction.getEndsAt().toInstant().toEpochMilli();

		try {
			stringRedisTemplate.opsForHash().putAll(
				stateKey,
				Map.of(
					"currentPrice", auction.getCurrentPrice().toPlainString(),
					"minimumBidAmount", auction.getMinimumBidAmount().toPlainString(),
					"highestBidderId", "",
					"endsAt", Long.toString(endsAtEpochMillis)
				)
			);
			stringRedisTemplate.opsForZSet().add(ENDING_KEY, auction.getAuctionId().toString(), endsAtEpochMillis);
		} catch (RedisConnectionFailureException exception) {
			// Local/test profiles may run without Redis; bid processing still requires Redis availability.
		}
	}

	public BidScriptResult bid(Long auctionId, BigDecimal bidPrice, Long bidderId) {
		String result = stringRedisTemplate.execute(
			BID_SCRIPT,
			List.of(stateKey(auctionId)),
			bidPrice.toPlainString(),
			bidderId.toString(),
			Long.toString(clock.millis())
		);
		if (result == null) {
			return new BidScriptResult(BidScriptStatus.AUCTION_ENDED, null, null);
		}
		if (result.startsWith("SUCCESS:")) {
			String[] parts = result.substring("SUCCESS:".length()).split(":", -1);
			return new BidScriptResult(
				BidScriptStatus.SUCCESS,
				parts[0].isBlank() ? null : Long.valueOf(parts[0]),
				parts.length < 2 || parts[1].isBlank() ? null : new BigDecimal(parts[1])
			);
		}
		if (result.startsWith("SAME_BIDDER:")) {
			return new BidScriptResult(BidScriptStatus.SAME_BIDDER, null, new BigDecimal(result.substring("SAME_BIDDER:".length())));
		}
		return new BidScriptResult(BidScriptStatus.valueOf(result), null, null);
	}

	public void restoreBidState(Long auctionId, BigDecimal failedBidPrice, Long failedBidderId, BigDecimal previousPrice, Long previousBidderId) {
		if (previousPrice == null) {
			return;
		}
		stringRedisTemplate.execute(
			RESTORE_BID_SCRIPT,
			List.of(stateKey(auctionId)),
			failedBidPrice.toPlainString(),
			failedBidderId.toString(),
			previousPrice.toPlainString(),
			previousBidderId == null ? "" : previousBidderId.toString()
		);
	}

	public Set<String> findEndingAuctionIds(long nowEpochMillis) {
		return stringRedisTemplate.opsForZSet().rangeByScore(ENDING_KEY, 0, nowEpochMillis);
	}

	public AuctionState getState(Long auctionId) {
		Map<Object, Object> state = stringRedisTemplate.opsForHash().entries(stateKey(auctionId));
		Object currentPrice = state.get("currentPrice");
		Object minimumBidAmount = state.get("minimumBidAmount");
		Object highestBidderId = state.get("highestBidderId");
		return new AuctionState(
			currentPrice == null ? null : new BigDecimal(currentPrice.toString()),
			minimumBidAmount == null ? null : new BigDecimal(minimumBidAmount.toString()),
			highestBidderId == null || highestBidderId.toString().isBlank() ? null : Long.valueOf(highestBidderId.toString())
		);
	}

	public void removeEnding(Long auctionId) {
		stringRedisTemplate.opsForZSet().remove(ENDING_KEY, auctionId.toString());
	}

	public void removeEndingValue(String auctionId) {
		stringRedisTemplate.opsForZSet().remove(ENDING_KEY, auctionId);
	}

	public void deleteState(Long auctionId) {
		stringRedisTemplate.delete(stateKey(auctionId));
	}

	public boolean markClosingSoonNotified(Long auctionId) {
		Long added = stringRedisTemplate.opsForSet().add(CLOSING_SOON_NOTIFIED_KEY, auctionId.toString());
		return added != null && added == 1;
	}

	public void removeClosingSoonNotified(Long auctionId) {
		stringRedisTemplate.opsForSet().remove(CLOSING_SOON_NOTIFIED_KEY, auctionId.toString());
	}

	public void refreshEnding(Auction auction) {
		try {
			initialize(auction);
		} catch (DataAccessException exception) {
			// Redis state is opportunistic for create/update paths; bid and end paths surface Redis failures.
		}
	}

	private String stateKey(Long auctionId) {
		return "auction:%d:state".formatted(auctionId);
	}

	public record BidScriptResult(BidScriptStatus status, Long previousBidderId, BigDecimal previousPrice) {
	}

	public record AuctionState(BigDecimal currentPrice, BigDecimal minimumBidAmount, Long highestBidderId) {
	}

	public enum BidScriptStatus {
		SUCCESS,
		AUCTION_ENDED,
		BID_TOO_LOW,
		SAME_BIDDER
	}
}
