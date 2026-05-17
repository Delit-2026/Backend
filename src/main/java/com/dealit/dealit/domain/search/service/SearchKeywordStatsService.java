package com.dealit.dealit.domain.search.service;

import com.dealit.dealit.domain.search.dto.PopularSearchKeywordListResponse;
import com.dealit.dealit.domain.search.dto.PopularSearchKeywordResponse;
import com.dealit.dealit.domain.search.dto.SearchResultType;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchKeywordStatsService {

	private static final String POPULAR_KEYWORDS_KEY_PREFIX = "search:popular:keywords:";
	private static final String ALL_KEY = POPULAR_KEYWORDS_KEY_PREFIX + "ALL";
	private static final int MAX_KEYWORD_LENGTH = 50;

	private final StringRedisTemplate stringRedisTemplate;

	public void record(String keyword, SearchResultType type) {
		String normalizedKeyword = normalize(keyword);
		if (normalizedKeyword == null) {
			return;
		}
		try {
			stringRedisTemplate.opsForZSet().incrementScore(ALL_KEY, normalizedKeyword, 1D);
			if (type != null) {
				stringRedisTemplate.opsForZSet().incrementScore(popularKeywordsKey(type), normalizedKeyword, 1D);
			}
		} catch (RedisConnectionFailureException ignored) {
			// Search should still work even if Redis keyword statistics are temporarily unavailable.
		}
	}

	public PopularSearchKeywordListResponse findPopularKeywords(SearchResultType type, int size) {
		int normalizedSize = Math.min(Math.max(size, 1), 50);
		try {
			List<PopularSearchKeywordResponse> content = stringRedisTemplate.opsForZSet()
				.reverseRangeWithScores(popularKeywordsKey(type), 0, normalizedSize - 1)
				.stream()
				.map(this::toResponse)
				.filter(Objects::nonNull)
				.toList();
			return new PopularSearchKeywordListResponse(content, normalizedSize);
		} catch (RedisConnectionFailureException ignored) {
			return new PopularSearchKeywordListResponse(List.of(), normalizedSize);
		}
	}

	private String popularKeywordsKey(SearchResultType type) {
		return type == null ? ALL_KEY : POPULAR_KEYWORDS_KEY_PREFIX + type.name();
	}

	private PopularSearchKeywordResponse toResponse(TypedTuple<String> tuple) {
		if (tuple == null || tuple.getValue() == null || tuple.getScore() == null) {
			return null;
		}
		return new PopularSearchKeywordResponse(tuple.getValue(), Math.round(tuple.getScore()));
	}

	private String normalize(String keyword) {
		if (keyword == null) {
			return null;
		}
		String normalizedKeyword = keyword.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
		if (normalizedKeyword.isBlank()) {
			return null;
		}
		if (normalizedKeyword.length() > MAX_KEYWORD_LENGTH) {
			return normalizedKeyword.substring(0, MAX_KEYWORD_LENGTH);
		}
		return normalizedKeyword;
	}
}
