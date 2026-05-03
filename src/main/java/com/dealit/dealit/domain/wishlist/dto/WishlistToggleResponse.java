package com.dealit.dealit.domain.wishlist.dto;

public record WishlistToggleResponse(
	Long productId,
	boolean liked,
	long favoriteCount
) {
}
