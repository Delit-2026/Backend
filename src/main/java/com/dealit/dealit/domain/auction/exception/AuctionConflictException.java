package com.dealit.dealit.domain.auction.exception;

import org.springframework.http.HttpStatus;

public class AuctionConflictException extends AuctionException {

	public AuctionConflictException(String message) {
		super("AUCTION_CONFLICT", message, HttpStatus.CONFLICT);
	}
}
