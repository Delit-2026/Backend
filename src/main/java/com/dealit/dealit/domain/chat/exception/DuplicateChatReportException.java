package com.dealit.dealit.domain.chat.exception;

public class DuplicateChatReportException extends RuntimeException {
    public DuplicateChatReportException() {
        super("이미 신고한 메시지입니다.");
    }

    public DuplicateChatReportException(String message) {
        super(message);
    }
}