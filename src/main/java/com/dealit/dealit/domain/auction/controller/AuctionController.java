package com.dealit.dealit.domain.auction.controller;

import com.dealit.dealit.domain.auction.dto.CategoryOptionResponse;
import com.dealit.dealit.domain.auction.dto.CreateAuctionRequest;
import com.dealit.dealit.domain.auction.dto.CreateAuctionResponse;
import com.dealit.dealit.domain.auction.dto.DeleteAuctionImageResponse;
import com.dealit.dealit.domain.auction.dto.RecommendCategoryRequest;
import com.dealit.dealit.domain.auction.dto.RecommendCategoryResponse;
import com.dealit.dealit.domain.auction.dto.RecommendPriceRequest;
import com.dealit.dealit.domain.auction.dto.RecommendPriceResponse;
import com.dealit.dealit.domain.auction.dto.SaveAuctionDraftRequest;
import com.dealit.dealit.domain.auction.dto.SaveAuctionDraftResponse;
import com.dealit.dealit.domain.auction.dto.UploadAuctionImageResponse;
import com.dealit.dealit.domain.auction.service.AuctionService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Auction", description = "경매 상품 등록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auction")
public class AuctionController {

	private final AuctionService auctionService;

	@Operation(summary = "경매 상품 이미지 업로드")
	@ApiResponse(
		responseCode = "200",
		description = "이미지 업로드 성공",
		content = @Content(schema = @Schema(implementation = UploadAuctionImageResponse.class))
	)
	@PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UploadAuctionImageResponse uploadImage(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestPart("file") MultipartFile file
	) {
		return auctionService.uploadImage(member.memberId(), file);
	}

	@Operation(summary = "경매 상품 이미지 삭제")
	@ApiResponse(
		responseCode = "200",
		description = "이미지 삭제 성공",
		content = @Content(schema = @Schema(implementation = DeleteAuctionImageResponse.class))
	)
	@DeleteMapping("/image/{imageId}")
	public DeleteAuctionImageResponse deleteImage(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long imageId
	) {
		return auctionService.deleteImage(member.memberId(), imageId);
	}

	@Operation(summary = "경매 상품 임시저장")
	@ApiResponse(
		responseCode = "200",
		description = "임시저장 성공",
		content = @Content(schema = @Schema(implementation = SaveAuctionDraftResponse.class))
	)
	@PostMapping("/draft")
	public SaveAuctionDraftResponse saveDraft(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestBody SaveAuctionDraftRequest request
	) {
		return auctionService.saveDraft(member.memberId(), request);
	}

	@Operation(summary = "카테고리 추천")
	@ApiResponse(
		responseCode = "200",
		description = "카테고리 추천 성공",
		content = @Content(schema = @Schema(implementation = RecommendCategoryResponse.class))
	)
	@PostMapping("/category/recommend")
	public RecommendCategoryResponse recommendCategory(@Valid @RequestBody RecommendCategoryRequest request) {
		return auctionService.recommendCategory(request);
	}

	@Operation(summary = "카테고리 목록 조회")
	@ApiResponse(
		responseCode = "200",
		description = "카테고리 조회 성공",
		content = @Content(schema = @Schema(implementation = CategoryOptionResponse.class))
	)
	@GetMapping("/categories")
	public List<CategoryOptionResponse> getCategories() {
		return auctionService.getCategories();
	}

	@Operation(summary = "가격 추천")
	@ApiResponse(
		responseCode = "200",
		description = "가격 추천 성공",
		content = @Content(schema = @Schema(implementation = RecommendPriceResponse.class))
	)
	@PostMapping("/price/recommend")
	public RecommendPriceResponse recommendPrice(@Valid @RequestBody RecommendPriceRequest request) {
		return auctionService.recommendPrice(request);
	}

	@Operation(summary = "경매 상품 등록")
	@ApiResponse(
		responseCode = "201",
		description = "경매 상품 등록 성공",
		content = @Content(schema = @Schema(implementation = CreateAuctionResponse.class))
	)
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreateAuctionResponse createAuction(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody CreateAuctionRequest request
	) {
		return auctionService.createAuction(member.memberId(), request);
	}
}
