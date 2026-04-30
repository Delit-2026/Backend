package com.dealit.dealit.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomUpdatedEvent(
        String type,
        ChatRoomListItemResponse room,
        LocalDateTime emittedAt
) {
    public static ChatRoomUpdatedEvent of(ChatRoomListItemResponse room, LocalDateTime emittedAt) {
        return new ChatRoomUpdatedEvent("chat.room.updated", room, emittedAt);
    }
}
