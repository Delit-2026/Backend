package com.dealit.dealit.domain.product.controller;

import com.dealit.dealit.domain.product.dto.ProductDetailResponse;
import com.dealit.dealit.domain.product.service.ProductDetailService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product Detail", description = "상품 상세 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductDetailController {

    private final ProductDetailService productDetailService;

    @Operation(
            summary = "상품 상세 조회",
            description = "productId로 일반 상품 또는 경매 상품의 상세 정보를 조회합니다. 로그인 사용자는 이메일 인증 여부와 관계없이 조회할 수 있습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "상품 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = ProductDetailResponse.class))
    )
    @ApiResponse(responseCode = "401", description = "인증 실패")
    @ApiResponse(responseCode = "404", description = "상품 없음")
    @GetMapping("/{productId}")
    public ProductDetailResponse getProductDetail(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long productId
    ) {
        return productDetailService.getProductDetail(member.memberId(), productId);
    }
}