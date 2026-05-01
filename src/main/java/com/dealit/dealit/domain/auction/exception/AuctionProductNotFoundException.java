package com.dealit.dealit.domain.auction.exception;

import org.springframework.http.HttpStatus;

public class AuctionProductNotFoundException extends AuctionException {

	public AuctionProductNotFoundException(String message) {
		super("AUCTION_PRODUCT_NOT_FOUND", message, HttpStatus.BAD_REQUEST);
	}
}
