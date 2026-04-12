package com.dealit.dealit.domain.chat.exception;

public class DuplicateChatRoomException extends RuntimeException {
    public DuplicateChatRoomException() {
        super("이미 존재하는 채팅방입니다.");
    }

    public DuplicateChatRoomException(String message) {
        super(message);
    }
}