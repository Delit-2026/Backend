package com.dealit.dealit.domain.auction.exception;

import org.springframework.http.HttpStatus;

public class AuctionAlreadyBiddedException extends AuctionException {

	public AuctionAlreadyBiddedException() {
		super(
			"AUCTION_ALREADY_BIDDED",
			"이미 입찰이 발생한 경매 상품은 수정하거나 삭제할 수 없습니다.",
			HttpStatus.CONFLICT
		);
	}
}
