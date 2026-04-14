package com.dealit.dealit.domain.auction.exception;

import org.springframework.http.HttpStatus;

public class InvalidAuctionRequestException extends AuctionException {

	public InvalidAuctionRequestException(String message) {
		super("INVALID_AUCTION_REQUEST", message, HttpStatus.BAD_REQUEST);
	}
}
