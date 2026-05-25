package com.dealit.dealit.domain.category.dto;

import java.util.List;

public record CategoryRecommendationResult(
	Long categoryId,
	String categoryName,
	List<Long> categoryPathIds,
	List<String> categoryNames,
	Double confidence,
	String reason,
	List<CategoryRecommendationAlternative> alternatives,
	String modelVersion
) {
}
