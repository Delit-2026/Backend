package com.dealit.dealit.domain.member.repository;

import com.dealit.dealit.domain.member.entity.MemberInterestCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberInterestCategoryRepository extends JpaRepository<MemberInterestCategory, Long> {

	List<MemberInterestCategory> findAllByMemberIdOrderByCategoryIdAsc(Long memberId);

	long countByMemberId(Long memberId);

	void deleteAllByMemberId(Long memberId);
}
