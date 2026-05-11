package com.dealit.dealit.domain.notification.repository;

import com.dealit.dealit.domain.notification.entity.InAppNotification;
import com.dealit.dealit.domain.notification.entity.InAppNotificationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

	Page<InAppNotification> findAllByMemberMemberIdAndDeletedAtIsNull(Long memberId, Pageable pageable);

	Optional<InAppNotification> findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(Long notificationId, Long memberId);

	long countByMemberMemberIdAndReadAtIsNullAndDeletedAtIsNull(Long memberId);

	@Query("""
		SELECT notification.type AS type, COUNT(notification) AS count
		FROM InAppNotification notification
		WHERE notification.member.memberId = :memberId
		  AND notification.readAt IS NULL
		  AND notification.deletedAt IS NULL
		GROUP BY notification.type
		""")
	List<UnreadTypeCount> countUnreadByType(@Param("memberId") Long memberId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		UPDATE InAppNotification notification
		SET notification.readAt = :readAt
		WHERE notification.member.memberId = :memberId
		  AND notification.readAt IS NULL
		  AND notification.deletedAt IS NULL
		""")
	int markAllAsReadByMemberId(@Param("memberId") Long memberId, @Param("readAt") LocalDateTime readAt);

	interface UnreadTypeCount {
		InAppNotificationType getType();

		long getCount();
	}
}
