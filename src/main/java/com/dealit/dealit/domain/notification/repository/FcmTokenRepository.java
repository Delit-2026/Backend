package com.dealit.dealit.domain.notification.repository;

import com.dealit.dealit.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

	Optional<FcmToken> findByToken(String token);

	Optional<FcmToken> findByMemberMemberIdAndTokenAndDeletedAtIsNull(Long memberId, String token);

	List<FcmToken> findAllByMemberMemberIdAndDeletedAtIsNull(Long memberId);
}
