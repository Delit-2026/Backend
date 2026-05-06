package com.dealit.dealit.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "내 관심 카테고리 수정 요청")
public record UpdateMyInterestCategoriesRequest(
	@Schema(description = "관심 카테고리 ID 목록(대분류만 허용)", example = "[1, 2]")
	List<Long> interestCategoryIds
) {
}
