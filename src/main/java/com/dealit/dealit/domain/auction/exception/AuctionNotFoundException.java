package com.dealit.dealit.domain.auction.exception;

import org.springframework.http.HttpStatus;

public class AuctionNotFoundException extends AuctionException {

	public AuctionNotFoundException(String message) {
		super("AUCTION_NOT_FOUND", message, HttpStatus.NOT_FOUND);
	}
}
