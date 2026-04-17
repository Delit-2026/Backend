package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	List<Category> findAllByOrderByDepthAscIdAsc();
}
