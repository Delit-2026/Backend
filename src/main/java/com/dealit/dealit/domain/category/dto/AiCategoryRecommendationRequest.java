package com.dealit.dealit.domain.category.dto;

import java.util.List;

public record AiCategoryRecommendationRequest(
	String title,
	String description,
	List<String> imageUrls,
	List<AiCategoryCandidateRequest> candidates
) {
}
