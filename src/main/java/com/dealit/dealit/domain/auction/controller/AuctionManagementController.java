package com.dealit.dealit.domain.auction.controller;

import com.dealit.dealit.domain.auction.dto.AuctionEditDetailResponse;
import com.dealit.dealit.domain.auction.dto.MySellingAuctionListResponse;
import com.dealit.dealit.domain.auction.dto.UpdateAuctionRequest;
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

import jakarta.validation.Valid;

@Tag(name = "Auction", description = "경매 상품 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuctionManagementController {

	private final AuctionService auctionService;

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

	@Operation(summary = "경매 수정용 상세 조회", description = "현재 사용자가 등록한 경매의 수정 화면 데이터를 조회한다.")
	@ApiResponse(responseCode = "200", description = "경매 수정용 상세 조회 성공",
		content = @Content(schema = @Schema(implementation = AuctionEditDetailResponse.class)))
	@GetMapping("/auctions/{auctionId}/edit")
	public AuctionEditDetailResponse getAuctionEditDetail(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId
	) {
		return auctionService.getAuctionEditDetail(member.memberId(), auctionId);
	}

	@Operation(summary = "경매 수정", description = "입찰이 없는 현재 사용자의 경매를 수정한다.")
	@ApiResponse(responseCode = "200", description = "경매 수정 성공",
		content = @Content(schema = @Schema(implementation = AuctionEditDetailResponse.class)))
	@ApiResponse(responseCode = "409", description = "입찰자가 있는 경매는 수정 불가")
	@PatchMapping("/auctions/{auctionId}")
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
	@DeleteMapping("/auctions/{auctionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteAuction(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long auctionId
	) {
		auctionService.deleteAuction(member.memberId(), auctionId);
	}
}
