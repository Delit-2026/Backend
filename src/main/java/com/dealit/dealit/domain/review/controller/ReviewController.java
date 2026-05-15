package com.dealit.dealit.domain.review.controller;

import com.dealit.dealit.domain.review.dto.CreateReviewRequest;
import com.dealit.dealit.domain.review.dto.ReviewListResponse;
import com.dealit.dealit.domain.review.dto.ReviewResponse;
import com.dealit.dealit.domain.review.service.ReviewService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Review", description = "거래 완료 후 작성하는 리뷰 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController {

	private final ReviewService reviewService;

	@Operation(
		summary = "리뷰 생성",
		description = "거래 완료 후 구매자가 판매자에게 리뷰와 별점을 작성합니다."
	)
	@PostMapping("/reviews")
	public ReviewResponse createReview(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody CreateReviewRequest request
	) {
		return reviewService.createReview(member.memberId(), request);
	}

	@Operation(
		summary = "받은 리뷰 조회",
		description = "회원 ID를 기준으로 해당 회원이 받은 리뷰 목록과 평균 별점을 조회합니다."
	)
	@GetMapping("/users/{memberId:\\d+}/reviews")
	public ReviewListResponse getUserReviews(
		@PathVariable Long memberId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return reviewService.getReceivedReviews(memberId, page, size);
	}

	@Operation(
		summary = "내가 받은 리뷰 조회",
		description = "로그인한 사용자가 받은 리뷰 목록과 평균 별점을 조회합니다."
	)
	@GetMapping("/users/me/reviews/received")
	public ReviewListResponse getMyReceivedReviews(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return reviewService.getReceivedReviews(member.memberId(), page, size);
	}

	@Operation(
		summary = "내가 작성한 리뷰 조회",
		description = "로그인한 사용자가 작성한 리뷰 목록을 조회합니다."
	)
	@GetMapping("/users/me/reviews/written")
	public ReviewListResponse getMyWrittenReviews(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return reviewService.getWrittenReviews(member.memberId(), page, size);
	}
}
