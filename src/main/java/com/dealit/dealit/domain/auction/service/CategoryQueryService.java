package com.dealit.dealit.domain.auction.service;

import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryService {

	public static final String ALL_CATEGORIES_CACHE = "categories:all:depth-asc-id-asc";

	private final CategoryRepository categoryRepository;

	@Cacheable(ALL_CATEGORIES_CACHE)
	public List<Category> findAllOrdered() {
		return categoryRepository.findAllByOrderByDepthAscIdAsc();
	}
}
