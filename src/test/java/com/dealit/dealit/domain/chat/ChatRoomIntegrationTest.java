package com.dealit.dealit.domain.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ChatRoomIntegrationTest {

    @Test
    @DisplayName("채팅방 테스트 컨텍스트 로드")
    void contextLoads() {
        // 최소 컨텍스트 로드 확인
    }
}