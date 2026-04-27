package com.dealit.dealit.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dealit.dealit.domain.member.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmail(String email);

	boolean existsByLoginId(String loginId);

	boolean existsByNickname(String nickname);

	boolean existsByNicknameAndMemberIdNot(String nickname, Long memberId);

	Optional<Member> findByEmailAndDeletedAtIsNull(String email);

	Optional<Member> findByLoginIdAndDeletedAtIsNull(String loginId);

	Optional<Member> findByMemberIdAndDeletedAtIsNull(Long memberId);
}
