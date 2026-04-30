package com.dealit.dealit.domain.chat.exception;

public class ChatForbiddenException extends RuntimeException {
    public ChatForbiddenException() {
        super("해당 채팅 리소스에 접근할 권한이 없습니다.");
    }

    public ChatForbiddenException(String message) {
        super(message);
    }
}