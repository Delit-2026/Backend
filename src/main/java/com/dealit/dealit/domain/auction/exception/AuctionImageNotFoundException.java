package com.dealit.dealit.domain.auction.exception;

import org.springframework.http.HttpStatus;

public class AuctionImageNotFoundException extends AuctionException {

	public AuctionImageNotFoundException(String message) {
		super("AUCTION_IMAGE_NOT_FOUND", message, HttpStatus.BAD_REQUEST);
	}
}
