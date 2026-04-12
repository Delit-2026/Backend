package com.dealit.dealit.domain.chat.repository;

import com.dealit.dealit.domain.chat.entity.ChatMessageReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageReportRepository extends JpaRepository<ChatMessageReport, Long> {

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM ChatMessageReport r
        WHERE r.messageId = :messageId
          AND r.reporterId = :reporterId
          AND r.deletedAt IS NULL
    """)
    boolean existsActiveReport(
            @Param("messageId") Long messageId,
            @Param("reporterId") Long reporterId
    );
}