package com.dealit.dealit.domain.product.controller;

import com.dealit.dealit.domain.product.dto.CategoryOptionResponse;
import com.dealit.dealit.domain.product.dto.CreateProductRequest;
import com.dealit.dealit.domain.product.dto.CreateProductResponse;
import com.dealit.dealit.domain.product.dto.DeleteProductImageResponse;
import com.dealit.dealit.domain.product.dto.HotListProductListResponse;
import com.dealit.dealit.domain.product.dto.PopularProductListResponse;
import com.dealit.dealit.domain.product.dto.ProductSearchListResponse;
import com.dealit.dealit.domain.product.dto.RecommendCategoryRequest;
import com.dealit.dealit.domain.product.dto.RecommendCategoryResponse;
import com.dealit.dealit.domain.product.dto.RecommendPriceRequest;
import com.dealit.dealit.domain.product.dto.RecommendPriceResponse;
import com.dealit.dealit.domain.product.dto.SaveProductDraftRequest;
import com.dealit.dealit.domain.product.dto.SaveProductDraftResponse;
import com.dealit.dealit.domain.product.dto.SearchCategoryOptionResponse;
import com.dealit.dealit.domain.product.dto.UploadProductImageResponse;
import com.dealit.dealit.domain.product.service.ProductService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Product", description = "일반 상품 등록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

	private final ProductService productService;

	@Operation(summary = "일반 상품 이미지 업로드")
	@ApiResponse(
		responseCode = "200",
		description = "이미지 업로드 성공",
		content = @Content(schema = @Schema(implementation = UploadProductImageResponse.class))
	)
	@PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UploadProductImageResponse uploadImage(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestPart("file") MultipartFile file
	) {
		return productService.uploadImage(member.memberId(), file);
	}

	@Operation(summary = "일반 상품 이미지 삭제")
	@ApiResponse(
		responseCode = "200",
		description = "이미지 삭제 성공",
		content = @Content(schema = @Schema(implementation = DeleteProductImageResponse.class))
	)
	@DeleteMapping("/image/{imageId}")
	public DeleteProductImageResponse deleteImage(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long imageId
	) {
		return productService.deleteImage(member.memberId(), imageId);
	}

	@Operation(summary = "일반 상품 임시저장")
	@ApiResponse(
		responseCode = "200",
		description = "임시저장 성공",
		content = @Content(schema = @Schema(implementation = SaveProductDraftResponse.class))
	)
	@PostMapping("/draft")
	public SaveProductDraftResponse saveDraft(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestBody SaveProductDraftRequest request
	) {
		return productService.saveDraft(member.memberId(), request);
	}

	@Operation(summary = "카테고리 추천")
	@ApiResponse(
		responseCode = "200",
		description = "카테고리 추천 성공",
		content = @Content(schema = @Schema(implementation = RecommendCategoryResponse.class))
	)
	@PostMapping("/category/recommend")
	public RecommendCategoryResponse recommendCategory(@Valid @RequestBody RecommendCategoryRequest request) {
		return productService.recommendCategory(request);
	}

	@Operation(summary = "카테고리 목록 조회")
	@ApiResponse(
		responseCode = "200",
		description = "카테고리 조회 성공",
		content = @Content(schema = @Schema(implementation = CategoryOptionResponse.class))
	)
	@GetMapping("/categories")
	public List<CategoryOptionResponse> getCategories() {
		return productService.getCategories();
	}

	@Operation(summary = "상품 검색 카테고리 목록 조회", description = "판매 중인 일반 상품이 있는 카테고리만 검색 가능 상태로 내려줍니다.")
	@ApiResponse(
		responseCode = "200",
		description = "상품 검색 카테고리 조회 성공",
		content = @Content(schema = @Schema(implementation = SearchCategoryOptionResponse.class))
	)
	@GetMapping("/search/categories")
	public List<SearchCategoryOptionResponse> getSearchCategories() {
		return productService.getSearchCategories();
	}

	@Operation(summary = "카테고리 기반 상품 검색")
	@ApiResponse(
		responseCode = "200",
		description = "카테고리 기반 상품 검색 성공",
		content = @Content(schema = @Schema(implementation = ProductSearchListResponse.class))
	)
	@GetMapping("/search")
	public ProductSearchListResponse searchProducts(
		@RequestParam Long categoryId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return productService.searchProductsByCategory(categoryId, page, size);
	}

	@Operation(summary = "가격 추천")
	@ApiResponse(
		responseCode = "200",
		description = "가격 추천 성공",
		content = @Content(schema = @Schema(implementation = RecommendPriceResponse.class))
	)
	@PostMapping("/price/recommend")
	public RecommendPriceResponse recommendPrice(@Valid @RequestBody RecommendPriceRequest request) {
		return productService.recommendPrice(request);
	}

	@Operation(summary = "일반 상품 등록")
	@ApiResponse(
		responseCode = "201",
		description = "일반 상품 등록 성공",
		content = @Content(schema = @Schema(implementation = CreateProductResponse.class))
	)
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreateProductResponse createProduct(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody CreateProductRequest request
	) {
		return productService.createProduct(member.memberId(), request);
	}

	@Operation(summary = "실시간 인기 일반 상품 목록 조회")
	@ApiResponse(
		responseCode = "200",
		description = "실시간 인기 일반 상품 목록 조회 성공",
		content = @Content(schema = @Schema(implementation = PopularProductListResponse.class))
	)
	@GetMapping("/popular")
	public PopularProductListResponse getPopularProducts(
		@RequestParam(defaultValue = "10") int size
	) {
		return productService.getPopularProducts(size);
	}

	@Operation(summary = "핫한 일반 상품 목록 조회")
	@ApiResponse(
		responseCode = "200",
		description = "핫한 일반 상품 목록 조회 성공",
		content = @Content(schema = @Schema(implementation = HotListProductListResponse.class))
	)
	@GetMapping("/hot-list")
	public HotListProductListResponse getHotListProducts(
		@RequestParam(defaultValue = "8") int size
	) {
		return productService.getHotListProducts(size);
	}
}
