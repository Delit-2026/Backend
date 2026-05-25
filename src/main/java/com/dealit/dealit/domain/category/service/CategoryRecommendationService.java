package com.dealit.dealit.domain.category.service;

import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auction.service.CategoryQueryService;
import com.dealit.dealit.domain.category.client.AiCategoryRecommendationClient;
import com.dealit.dealit.domain.category.dto.AiCategoryCandidateRequest;
import com.dealit.dealit.domain.category.dto.AiCategoryRecommendationAlternativeResponse;
import com.dealit.dealit.domain.category.dto.AiCategoryRecommendationRequest;
import com.dealit.dealit.domain.category.dto.AiCategoryRecommendationResponse;
import com.dealit.dealit.domain.category.dto.CategoryRecommendationAlternative;
import com.dealit.dealit.domain.category.dto.CategoryRecommendationResult;
import com.dealit.dealit.domain.category.exception.CategoryRecommendationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryRecommendationService {

	private static final int TOP_CATEGORY_DEPTH = 1;
	private static final int LEAF_CATEGORY_DEPTH = 3;
	private static final double RETRY_CONFIDENCE_THRESHOLD = 0.8;

	private final CategoryQueryService categoryQueryService;
	private final AiCategoryRecommendationClient aiCategoryRecommendationClient;

	public CategoryRecommendationResult recommend(
		String title,
		String description,
		Long topCategoryId,
		List<String> imageUrls
	) {
		List<Category> categories = categoryQueryService.findAllOrdered();
		Map<Long, Category> categoriesById = toCategoriesById(categories);
		Category topCategory = categoriesById.get(topCategoryId);
		if (topCategory == null) {
			throw new IllegalArgumentException("존재하지 않는 대분류 카테고리입니다.");
		}
		if (topCategory.getDepth() != TOP_CATEGORY_DEPTH) {
			throw new IllegalArgumentException("카테고리 추천에는 대분류 카테고리 ID가 필요합니다.");
		}

		List<Category> leafCategories = categories.stream()
			.filter(category -> category.getDepth() == LEAF_CATEGORY_DEPTH)
			.filter(category -> isDescendantOf(category, topCategory.getId(), categoriesById))
			.toList();
		if (leafCategories.isEmpty()) {
			throw new IllegalArgumentException("선택한 대분류에 추천 가능한 하위 카테고리가 없습니다.");
		}

		List<AiCategoryCandidateRequest> candidates = leafCategories.stream()
			.map(category -> toAiCandidate(category, categoriesById))
			.toList();
		Set<Long> candidateIds = new LinkedHashSet<>(leafCategories.stream().map(Category::getId).toList());

		AiCategoryRecommendationRequest aiRequest = new AiCategoryRecommendationRequest(
			title,
			description,
			normalizeImageUrls(imageUrls),
			candidates
		);
		AiCategoryRecommendationResponse aiResponse = recommendWithLowConfidenceRetry(aiRequest);

		Long recommendedCategoryId = aiResponse.recommendedCategoryId();
		if (!candidateIds.contains(recommendedCategoryId)) {
			throw new CategoryRecommendationException(
				"CATEGORY_RECOMMENDATION_INVALID_RESPONSE",
				"AI가 후보에 없는 카테고리를 추천했습니다.",
				HttpStatus.BAD_GATEWAY
			);
		}

		Category recommendedCategory = categoriesById.get(recommendedCategoryId);
		List<Category> categoryPath = buildCategoryPath(recommendedCategory, categoriesById);
		return new CategoryRecommendationResult(
			recommendedCategory.getId(),
			recommendedCategory.getNameKo(),
			categoryPath.stream().map(Category::getId).toList(),
			categoryPath.stream().map(Category::getNameKo).toList(),
			aiResponse.confidence(),
			aiResponse.reason(),
			toAlternatives(aiResponse.alternatives(), candidateIds, categoriesById),
			aiResponse.modelVersion()
		);
	}

	private AiCategoryRecommendationResponse recommendWithLowConfidenceRetry(
		AiCategoryRecommendationRequest request
	) {
		AiCategoryRecommendationResponse firstResponse = aiCategoryRecommendationClient.recommend(request);
		if (confidenceOf(firstResponse) >= RETRY_CONFIDENCE_THRESHOLD) {
			return firstResponse;
		}

		AiCategoryRecommendationResponse secondResponse = aiCategoryRecommendationClient.recommend(request);
		return confidenceOf(secondResponse) > confidenceOf(firstResponse)
			? secondResponse
			: firstResponse;
	}

	private double confidenceOf(AiCategoryRecommendationResponse response) {
		if (response == null || response.confidence() == null) {
			return 0.0;
		}
		return response.confidence();
	}

	private Map<Long, Category> toCategoriesById(List<Category> categories) {
		Map<Long, Category> categoriesById = new LinkedHashMap<>();
		for (Category category : categories) {
			categoriesById.put(category.getId(), category);
		}
		return categoriesById;
	}

	private boolean isDescendantOf(Category category, Long ancestorId, Map<Long, Category> categoriesById) {
		Long parentId = category.getParentId();
		while (parentId != null) {
			if (parentId.equals(ancestorId)) {
				return true;
			}
			Category parent = categoriesById.get(parentId);
			parentId = parent == null ? null : parent.getParentId();
		}
		return false;
	}

	private AiCategoryCandidateRequest toAiCandidate(Category category, Map<Long, Category> categoriesById) {
		List<Category> path = buildCategoryPath(category, categoriesById);
		return new AiCategoryCandidateRequest(
			category.getId(),
			joinPath(path.stream().map(Category::getNameKo).toList()),
			joinPath(path.stream().map(Category::getNameEn).toList())
		);
	}

	private List<Category> buildCategoryPath(Category category, Map<Long, Category> categoriesById) {
		List<Category> path = new ArrayList<>();
		Category current = category;
		while (current != null) {
			path.add(current);
			current = current.getParentId() == null ? null : categoriesById.get(current.getParentId());
		}
		Collections.reverse(path);
		return path;
	}

	private String joinPath(List<String> names) {
		return String.join(" > ", names);
	}

	private List<String> normalizeImageUrls(List<String> imageUrls) {
		if (imageUrls == null || imageUrls.isEmpty()) {
			return List.of();
		}

		return imageUrls.stream()
			.filter(imageUrl -> imageUrl != null && !imageUrl.isBlank())
			.map(String::trim)
			.limit(5)
			.toList();
	}

	private List<CategoryRecommendationAlternative> toAlternatives(
		List<AiCategoryRecommendationAlternativeResponse> alternatives,
		Set<Long> candidateIds,
		Map<Long, Category> categoriesById
	) {
		if (alternatives == null || alternatives.isEmpty()) {
			return List.of();
		}

		return alternatives.stream()
			.filter(alternative -> alternative.categoryId() != null)
			.filter(alternative -> candidateIds.contains(alternative.categoryId()))
			.map(alternative -> {
				Category category = categoriesById.get(alternative.categoryId());
				return new CategoryRecommendationAlternative(
					alternative.categoryId(),
					category == null ? null : category.getNameKo(),
					alternative.confidence()
				);
			})
			.toList();
	}
}
