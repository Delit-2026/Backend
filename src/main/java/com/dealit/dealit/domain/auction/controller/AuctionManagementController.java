package com.dealit.dealit.domain.auction.controller;

import com.dealit.dealit.domain.auction.dto.AuctionDetailResponse;
import com.dealit.dealit.domain.auction.dto.AuctionBidHistoryResponse;
import com.dealit.dealit.domain.auction.dto.AuctionListResponse;
import com.dealit.dealit.domain.auction.dto.BidRequest;
import com.dealit.dealit.domain.auction.dto.BidResponse;
import com.dealit.dealit.domain.auction.dto.AuctionEditDetailResponse;
import com.dealit.dealit.domain.auction.dto.CreateAuctionRequest;
import com.dealit.dealit.domain.auction.dto.DeclineReauctionResponse;
import com.dealit.dealit.domain.auction.dto.MyBuyingAuctionListResponse;
import com.dealit.dealit.domain.auction.dto.MySellingAuctionListResponse;
import com.dealit.dealit.domain.auction.dto.ReauctionPreviewResponse;
import com.dealit.dealit.domain.auction.dto.ReauctionResponse;
import com.dealit.dealit.domain.auction.dto.SearchCategoryOptionResponse;
import com.dealit.dealit.domain.auction.dto.UpdateAuctionRequest;
import com.dealit.dealit.domain.auction.service.AuctionBidService;
import com.dealit.dealit.domain.auction.service.AuctionBuyingService;
import com.dealit.dealit.domain.auction.service.AuctionService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

import java.util.List;

@Tag(name = "Auction", description = "경매 상품 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuctionManagementController {

	private final AuctionService auctionService;
	private final AuctionBidService auctionBidService;
	private final AuctionBuyingService auctionBuyingService;

	@Operation(summary = "실시간 인기 경매 목록 조회", description = "입찰 수를 등록 후 경과 시간으로 나눈 점수 기준으로 진행 중인 경매를 조회한다.")
	@ApiResponse(responseCode = "200", description = "실시간 인기 경매 목록 조회 성공",
		content = @Content(schema = @Schema(implementation = AuctionListResponse.class)))
	@GetMapping("/auctions/popular")
	public AuctionListResponse getPopularAuctions(@RequestParam(defaultValue = "4") int limit) {
		return auctionService.getPopularAuctions(limit);
	}

	@Operation(summary = "마감임박 경매 목록 조회", description = "현재 서버 시간 기준 종료 시각이 가까운 진행 중 경매를 조회한다.")
	@ApiResponse(responseCode = "200", description = "마감임박 경매 목록 조회 성공",
		content = @Content(schema = @Schema(implementation = AuctionListResponse.class)))
	@GetMapping("/auctions/closing-soon")
	public AuctionListResponse getClosingSoonAuctions(@RequestParam(defaultValue = "3") int limit) {
		return auctionService.getClosingSoonAuctions(limit);
	}

	@Operation(summary = "경매 검색 카테고리 목록 조회", description = "진행 중인 경매가 있는 카테고리 정보를 함께 내려줍니다.")
	@ApiResponse(responseCode = "200", description = "경매 검색 카테고리 조회 성공",
		content = @Content(schema = @Schema(implementation = SearchCategoryOptionResponse.class)))
	@GetMapping("/auctions/search/categories")
	public List<SearchCategoryOptionResponse> getAuctionSearchCategories() {
		return auctionService.getSearchCategories();
	}

	@Operation(summary = "카테고리 기반 경매 검색")
	@ApiResponse(responseCode = "200", description = "카테고리 기반 경매 검색 성공",
		content = @Content(schema = @Schema(implementation = AuctionListResponse.class)))
	@GetMapping("/auctions/search")
	public AuctionListResponse searchAuctions(
		@RequestParam Long categoryId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return auctionService.searchAuctionsByCategory(categoryId, page, size);
	}

	@Operation(summary = "경매 상세 조회", description = "경매 현재가와 서버 시간을 조회한다.")
	@GetMapping("/auctions/{auctionId:\\d+}")
	public AuctionDetailResponse getAuction(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId
	) {
		return auctionBidService.getAuction(auctionId, member == null ? null : member.memberId());
	}

	@Operation(summary = "경매 입찰", description = "서버 도착 시간 기준으로 입찰을 처리한다.")
	@PostMapping("/auctions/{auctionId:\\d+}/bids")
	public BidResponse bid(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId,
		@Valid @RequestBody BidRequest request
	) {
		return auctionBidService.bid(auctionId, member.memberId(), request.bidPrice());
	}

	@Operation(summary = "경매 입찰 현황 조회", description = "경매 현재가와 입찰 내역을 최신순으로 조회한다.")
	@GetMapping("/auctions/{auctionId:\\d+}/bids")
	public AuctionBidHistoryResponse getBidHistory(@PathVariable Long auctionId) {
		return auctionBidService.getBidHistory(auctionId);
	}

	@Operation(summary = "내 판매중 경매 목록", description = "마이페이지 판매중 화면에서 현재 사용자의 진행중 경매 목록을 페이징 조회한다.")
	@ApiResponse(responseCode = "200", description = "내 판매중 경매 목록 조회 성공",
		content = @Content(schema = @Schema(implementation = MySellingAuctionListResponse.class)))
	@GetMapping("/mypage/auctions/selling")
	public MySellingAuctionListResponse getMySellingAuctions(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return auctionService.getMySellingAuctionProducts(member.memberId(), page, size);
	}

	@Operation(summary = "내 구매중 경매 목록", description = "마이페이지 구매중 화면에서 현재 사용자가 입찰한 경매 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "내 구매중 경매 목록 조회 성공",
		content = @Content(schema = @Schema(implementation = MyBuyingAuctionListResponse.class)))
	@GetMapping("/mypage/auctions/buying")
	public MyBuyingAuctionListResponse getMyBuyingAuctions(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return auctionBuyingService.getMyBuyingAuctions(member.memberId(), page, size);
	}

	@Operation(summary = "내 구매중 종료 경매 숨김", description = "마이페이지 구매중 화면에서 종료된 경매 내역을 현재 사용자에게만 숨깁니다.")
	@ApiResponse(responseCode = "204", description = "내 구매중 종료 경매 숨김 성공")
	@ApiResponse(responseCode = "409", description = "진행 중인 경매는 숨김 불가")
	@DeleteMapping("/mypage/auctions/buying/{auctionId:\\d+}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void hideEndedBuyingAuction(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId
	) {
		auctionBuyingService.hideEndedBuyingAuction(member.memberId(), auctionId);
	}

	@Operation(summary = "경매 수정용 상세 조회", description = "현재 사용자가 등록한 경매의 수정 화면 데이터를 조회한다.")
	@ApiResponse(responseCode = "200", description = "경매 수정용 상세 조회 성공",
		content = @Content(schema = @Schema(implementation = AuctionEditDetailResponse.class)))
	@GetMapping("/auctions/{auctionId:\\d+}/edit")
	public AuctionEditDetailResponse getAuctionEditDetail(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId
	) {
		return auctionService.getAuctionEditDetail(member.memberId(), auctionId);
	}

	@Operation(summary = "재경매 미리보기", description = "유찰된 본인 경매를 재등록하기 위한 기존 상품 정보를 조회한다.")
	@GetMapping("/auctions/{auctionId:\\d+}/reauction-preview")
	public ReauctionPreviewResponse getReauctionPreview(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId
	) {
		return auctionService.getReauctionPreview(member.memberId(), auctionId);
	}

	@Operation(summary = "재경매 등록", description = "유찰된 본인 경매를 바탕으로 새 경매를 등록한다.")
	@PostMapping("/auctions/{auctionId:\\d+}/reauction")
	@ResponseStatus(HttpStatus.CREATED)
	public ReauctionResponse reauction(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId,
		@Valid @RequestBody CreateAuctionRequest request
	) {
		return auctionService.reauction(member.memberId(), auctionId, request);
	}

	@Operation(summary = "재경매 거절", description = "유찰된 경매의 재등록 대기를 종료한다.")
	@PatchMapping("/auctions/{auctionId:\\d+}/reauction/decline")
	public DeclineReauctionResponse declineReauction(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId
	) {
		return auctionService.declineReauction(member.memberId(), auctionId);
	}

	@Operation(summary = "경매 수정", description = "입찰이 없는 현재 사용자의 경매를 수정한다.")
	@ApiResponse(responseCode = "200", description = "경매 수정 성공",
		content = @Content(schema = @Schema(implementation = AuctionEditDetailResponse.class)))
	@ApiResponse(responseCode = "409", description = "입찰자가 있는 경매는 수정 불가")
	@PatchMapping("/auctions/{auctionId:\\d+}")
	public AuctionEditDetailResponse updateAuction(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId,
		@Valid @RequestBody UpdateAuctionRequest request
	) {
		return auctionService.updateAuction(member.memberId(), auctionId, request);
	}

	@Operation(summary = "경매 삭제", description = "입찰이 없는 현재 사용자의 경매를 삭제한다.")
	@ApiResponse(responseCode = "204", description = "경매 삭제 성공")
	@ApiResponse(responseCode = "409", description = "입찰자가 있는 경매는 삭제 불가")
	@DeleteMapping("/auctions/{auctionId:\\d+}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteAuction(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId
	) {
		auctionService.deleteAuction(member.memberId(), auctionId);
	}
}
