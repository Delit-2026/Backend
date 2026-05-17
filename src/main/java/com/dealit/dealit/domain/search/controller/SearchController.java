package com.dealit.dealit.domain.search.controller;

import com.dealit.dealit.domain.search.dto.PopularSearchKeywordListResponse;
import com.dealit.dealit.domain.search.dto.SearchListResponse;
import com.dealit.dealit.domain.search.dto.SearchReindexResponse;
import com.dealit.dealit.domain.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search", description = "통합 검색 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SearchController {

	private final SearchService searchService;

	@Operation(
		summary = "Text Search",
		description = "keyword, categoryId 조건으로 판매 중인 일반 상품과 진행 중인 경매를 OpenSearch에서 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "통합 검색 성공")
	@GetMapping("/search")
	public SearchListResponse search(
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) Long categoryId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return searchService.search(keyword, categoryId, page, size);
	}

	@Operation(
		summary = "Popular Search Keywords",
		description = "사용자가 검색한 keyword 횟수를 기준으로 인기 검색어를 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "인기 검색어 조회 성공")
	@GetMapping("/search/popular")
	public PopularSearchKeywordListResponse popularKeywords(
		@RequestParam(defaultValue = "10") int size
	) {
		return searchService.findPopularKeywords(size);
	}

	@Operation(
		summary = "Rebuild Search Index",
		description = "판매 중인 일반 상품과 진행 중인 경매를 OpenSearch 검색 문서로 다시 색인합니다."
	)
	@ApiResponse(responseCode = "200", description = "검색 인덱스 재생성 성공")
	@PostMapping("/search/index/rebuild")
	public SearchReindexResponse rebuildIndex() {
		return searchService.rebuildIndex();
	}
}
