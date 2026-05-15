package com.dealit.dealit.domain.auction.exception;

import org.springframework.http.HttpStatus;

public class AuctionBidException extends AuctionException {

	private AuctionBidException(String code, String message, HttpStatus status) {
		super(code, message, status);
	}

	public static AuctionBidException auctionEnded() {
		return new AuctionBidException("AUCTION_ENDED", "이미 종료된 경매입니다.", HttpStatus.CONFLICT);
	}

	public static AuctionBidException priceBelowMinimum() {
		return new AuctionBidException(
			"BID_PRICE_BELOW_MINIMUM",
			"최소 입찰 금액을 충족해야 합니다.",
			HttpStatus.BAD_REQUEST
		);
	}

	public static AuctionBidException priceChanged() {
		return new AuctionBidException(
			"BID_PRICE_CHANGED",
			"최고 입찰가가 변경되었습니다. 다시 확인 후 입찰해주세요.",
			HttpStatus.CONFLICT
		);
	}
}
