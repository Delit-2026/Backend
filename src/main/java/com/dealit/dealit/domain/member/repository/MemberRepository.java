package com.dealit.dealit.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dealit.dealit.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmail(String email);

	boolean existsByLoginId(String loginId);
}
