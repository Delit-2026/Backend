package com.dealit.dealit.domain.product.controller;

import com.dealit.dealit.domain.product.dto.MySellingProductListResponse;
import com.dealit.dealit.domain.product.dto.ProductEditDetailResponse;
import com.dealit.dealit.domain.product.dto.UpdateProductRequest;
import com.dealit.dealit.domain.product.service.ProductService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product", description = "일반 상품 판매 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProductManagementController {

	private final ProductService productService;

	@Operation(summary = "내 판매 중 일반 상품 목록", description = "마이페이지 판매 중 화면에서 현재 사용자의 판매 중 일반 상품 목록을 페이지 단위로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "내 판매 중 일반 상품 목록 조회 성공")
	@GetMapping("/mypage/products/selling")
	public MySellingProductListResponse getMySellingProducts(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return productService.getMySellingProducts(member.memberId(), page, size);
	}

	@Operation(summary = "일반 상품 수정 상세 조회", description = "현재 사용자가 등록한 일반 상품의 수정 화면 데이터를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "일반 상품 수정 상세 조회 성공")
	@GetMapping("/products/{productId}/edit")
	public ProductEditDetailResponse getProductEditDetail(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long productId
	) {
		return productService.getProductEditDetail(member.memberId(), productId);
	}

	@Operation(summary = "일반 상품 수정", description = "현재 사용자가 등록한 판매 중 일반 상품을 수정합니다.")
	@ApiResponse(responseCode = "200", description = "일반 상품 수정 성공")
	@PatchMapping("/products/{productId}")
	public ProductEditDetailResponse updateProduct(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long productId,
		@Valid @RequestBody UpdateProductRequest request
	) {
		return productService.updateProduct(member.memberId(), productId, request);
	}

	@Operation(summary = "일반 상품 삭제", description = "현재 사용자가 등록한 판매 중 일반 상품을 삭제합니다.")
	@ApiResponse(responseCode = "204", description = "일반 상품 삭제 성공")
	@DeleteMapping("/products/{productId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProduct(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long productId
	) {
		productService.deleteProduct(member.memberId(), productId);
	}
}
