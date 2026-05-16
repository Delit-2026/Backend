package com.dealit.dealit.domain.wishlist.controller;

import com.dealit.dealit.domain.wishlist.dto.MyAuctionWishlistListResponse;
import com.dealit.dealit.domain.wishlist.dto.MyWishlistListResponse;
import com.dealit.dealit.domain.wishlist.dto.WishlistToggleResponse;
import com.dealit.dealit.domain.wishlist.service.WishlistService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wishlist", description = "찜 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class WishlistController {

	private final WishlistService wishlistService;

	@Operation(summary = "찜 추가")
	@ApiResponse(responseCode = "200", description = "찜 추가 성공")
	@PostMapping("/wishlist/{productId}")
	public WishlistToggleResponse addWishlist(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long productId
	) {
		return wishlistService.addWishlist(member.memberId(), productId);
	}

	@Operation(summary = "찜 취소")
	@ApiResponse(responseCode = "200", description = "찜 취소 성공")
	@DeleteMapping("/wishlist/{productId}")
	public WishlistToggleResponse removeWishlist(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long productId
	) {
		return wishlistService.removeWishlist(member.memberId(), productId);
	}

	@Operation(summary = "내 찜 목록 조회")
	@ApiResponse(responseCode = "200", description = "내 찜 목록 조회 성공")
	@GetMapping("/mypage/wishlist")
	public MyWishlistListResponse getMyWishlist(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return wishlistService.getMyWishlist(member.memberId(), page, size);
	}

	@Operation(summary = "내 경매 찜 목록 조회")
	@ApiResponse(responseCode = "200", description = "내 경매 찜 목록 조회 성공")
	@GetMapping("/mypage/wishlist/auctions")
	public MyAuctionWishlistListResponse getMyAuctionWishlist(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return wishlistService.getMyAuctionWishlist(member.memberId(), page, size);
	}
}
