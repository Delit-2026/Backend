package com.dealit.dealit.domain.search.service;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.repository.AuctionRepository;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.search.document.SearchDocument;
import com.dealit.dealit.domain.search.dto.PopularSearchKeywordListResponse;
import com.dealit.dealit.domain.search.dto.SearchListResponse;
import com.dealit.dealit.domain.search.dto.SearchReindexResponse;
import com.dealit.dealit.domain.search.dto.SearchResultType;
import com.dealit.dealit.domain.search.service.SearchDocumentFactory.CategoryNode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

	private static final int REINDEX_BATCH_SIZE = 200;

	private final ProductRepository productRepository;
	private final AuctionRepository auctionRepository;
	private final OpenSearchClient openSearchClient;
	private final SearchDocumentFactory searchDocumentFactory;
	private final SearchKeywordStatsService searchKeywordStatsService;
	private final Clock clock;

	public SearchListResponse search(String keyword, SearchResultType type, Long categoryId, int page, int size) {
		String normalizedKeyword = normalizeKeyword(keyword);
		validateSearchCondition(normalizedKeyword, categoryId);
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = Math.min(Math.max(size, 1), 100);
		searchKeywordStatsService.record(normalizedKeyword, type);
		return openSearchClient.search(normalizedKeyword, type, categoryId, normalizedPage, normalizedSize);
	}

	public PopularSearchKeywordListResponse findPopularKeywords(SearchResultType type, int size) {
		return searchKeywordStatsService.findPopularKeywords(type, size);
	}

	public SearchReindexResponse rebuildIndex() {
		openSearchClient.recreateIndex();
		Map<Long, CategoryNode> categoryNodes = searchDocumentFactory.loadCategoryNodes();
		int indexedCount = 0;
		indexedCount += reindexRegularProducts(categoryNodes);
		indexedCount += reindexOngoingAuctions(categoryNodes);
		return new SearchReindexResponse(openSearchClient.indexName(), indexedCount);
	}

	private int reindexRegularProducts(Map<Long, CategoryNode> categoryNodes) {
		int indexedCount = 0;
		int page = 0;
		Page<Product> products;
		do {
			products = productRepository.findPageBySaleTypeAndStatusAndDeletedAtIsNull(
				ProductSaleType.REGULAR,
				ProductStatus.ON_SALE,
				PageRequest.of(page, REINDEX_BATCH_SIZE)
			);
			List<SearchDocument> documents = products.stream()
				.map(product -> searchDocumentFactory.regularProduct(product, categoryNodes))
				.toList();
			indexedCount += openSearchClient.bulkIndex(documents);
			page++;
		} while (products.hasNext());
		return indexedCount;
	}

	private int reindexOngoingAuctions(Map<Long, CategoryNode> categoryNodes) {
		int indexedCount = 0;
		int page = 0;
		OffsetDateTime now = OffsetDateTime.now(clock);
		Page<Auction> auctions;
		do {
			auctions = auctionRepository.findPageByStatusAndEndsAtAfterAndDeletedAtIsNullAndProductDeletedAtIsNull(
				AuctionStatus.ONGOING,
				now,
				PageRequest.of(page, REINDEX_BATCH_SIZE)
			);
			List<SearchDocument> documents = auctions.stream()
				.map(auction -> searchDocumentFactory.auction(auction, categoryNodes))
				.toList();
			indexedCount += openSearchClient.bulkIndex(documents);
			page++;
		} while (auctions.hasNext());
		return indexedCount;
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
