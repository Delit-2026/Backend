package com.dealit.dealit.domain.auction.exception;

import org.springframework.http.HttpStatus;

public class AuctionAccessDeniedException extends AuctionException {

	public AuctionAccessDeniedException(String message) {
		super("AUCTION_ACCESS_DENIED", message, HttpStatus.FORBIDDEN);
	}
}
