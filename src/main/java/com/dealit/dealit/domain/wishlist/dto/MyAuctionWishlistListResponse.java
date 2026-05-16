package com.dealit.dealit.domain.wishlist.dto;

import java.util.List;

public record MyAuctionWishlistListResponse(
	List<MyAuctionWishlistItemResponse> content,
	int page,
	int size,
	long totalElements,
	boolean hasNext
) {
}
