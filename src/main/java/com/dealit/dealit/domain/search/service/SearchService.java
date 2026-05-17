package com.dealit.dealit.domain.search.service;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.repository.AuctionRepository;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.search.document.SearchDocument;
import com.dealit.dealit.domain.search.dto.PopularSearchKeywordListResponse;
import com.dealit.dealit.domain.search.dto.SearchListResponse;
import com.dealit.dealit.domain.search.dto.SearchReindexResponse;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

	private final ProductRepository productRepository;
	private final AuctionRepository auctionRepository;
	private final OpenSearchClient openSearchClient;
	private final SearchDocumentFactory searchDocumentFactory;
	private final SearchKeywordStatsService searchKeywordStatsService;
	private final Clock clock;

	public SearchListResponse search(String keyword, Long categoryId, int page, int size) {
		String normalizedKeyword = normalizeKeyword(keyword);
		validateSearchCondition(normalizedKeyword, categoryId);
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = Math.min(Math.max(size, 1), 100);
		searchKeywordStatsService.record(normalizedKeyword);
		return openSearchClient.search(normalizedKeyword, categoryId, normalizedPage, normalizedSize);
	}

	public PopularSearchKeywordListResponse findPopularKeywords(int size) {
		return searchKeywordStatsService.findPopularKeywords(size);
	}

	public SearchReindexResponse rebuildIndex() {
		List<SearchDocument> documents = new ArrayList<>();

		productRepository.findAllBySaleTypeAndStatusAndDeletedAtIsNull(
				ProductSaleType.REGULAR,
				ProductStatus.ON_SALE
			)
			.stream()
			.map(searchDocumentFactory::regularProduct)
			.forEach(documents::add);

		auctionRepository.findAllByStatusAndEndsAtAfterAndDeletedAtIsNullAndProductDeletedAtIsNull(
				AuctionStatus.ONGOING,
				OffsetDateTime.now(clock)
			)
			.stream()
			.map(searchDocumentFactory::auction)
			.forEach(documents::add);

		int indexedCount = openSearchClient.rebuild(documents);
		return new SearchReindexResponse(openSearchClient.indexName(), indexedCount);
	}

	private String normalizeKeyword(String keyword) {
		return keyword == null ? null : keyword.trim();
	}

	private void validateSearchCondition(String keyword, Long categoryId) {
		if ((keyword == null || keyword.isBlank()) && categoryId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword or categoryId is required.");
		}
		if (categoryId != null && categoryId <= 0L) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId must be positive.");
		}
	}
}
