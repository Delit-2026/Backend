package com.dealit.dealit.domain.chat.entity;

import com.dealit.dealit.global.entity.BaseEntity; // TODO: 실제 BaseEntity 경로로 수정
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "chat_message_reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_message_report_message_reporter",
                        columnNames = {"message_id", "reporter_id"}
                )
        },
        indexes = {
                @Index(name = "idx_chat_report_message", columnList = "message_id"),
                @Index(name = "idx_chat_report_reporter", columnList = "reporter_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    public static ChatMessageReport create(Long messageId, Long reporterId, String reason) {
        ChatMessageReport report = new ChatMessageReport();
        report.messageId = messageId;
        report.reporterId = reporterId;
        report.reason = reason;
        report.reportedAt = LocalDateTime.now();
        return report;
    }
}