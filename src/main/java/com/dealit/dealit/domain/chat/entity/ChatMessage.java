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
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_message_room_sent_at", columnList = "room_id,sent_at"),
                @Index(name = "idx_chat_message_room_is_read", columnList = "room_id,is_read")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_nickname", nullable = false, length = 50)
    private String senderNickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ChatMessageType messageType;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public static ChatMessage create(
            Long roomId,
            Long senderId,
            String senderNickname,
            ChatMessageType messageType,
            String content
    ) {
        ChatMessage message = new ChatMessage();
        message.roomId = roomId;
        message.senderId = senderId;
        message.senderNickname = senderNickname;
        message.messageType = messageType;
        message.content = content;
        message.isRead = false;
        message.sentAt = LocalDateTime.now();
        return message;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}