package com.dealit.dealit.global.security.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.chat.entity.ChatRoom;
import com.dealit.dealit.domain.chat.entity.ChatType;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.global.security.AuthenticatedMember;
import com.dealit.dealit.global.security.jwt.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class WebSocketAuthChannelInterceptorTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(
            jwtService,
            memberRepository,
            chatRoomRepository
    );

    @Test
    void subscribeToAccessibleChatRoomSucceeds() {
        Long roomId = 10L;
        Long userId = 1L;
        when(chatRoomRepository.findAccessibleRoom(roomId, userId))
                .thenReturn(Optional.of(ChatRoom.create(userId, 2L, 100L, ChatType.GENERAL)));

        Message<byte[]> message = subscribeMessage("/topic/chats/rooms/" + roomId, userId);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
    }

    @Test
    void subscribeToInaccessibleChatRoomFails() {
        Long roomId = 10L;
        Long userId = 1L;
        when(chatRoomRepository.findAccessibleRoom(roomId, userId))
                .thenReturn(Optional.empty());

        Message<byte[]> message = subscribeMessage("/topic/chats/rooms/" + roomId, userId);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("접근 권한이 없는 채팅방입니다.");
    }

    private Message<byte[]> subscribeMessage(String destination, Long userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                new AuthenticatedMember(userId, "user-" + userId, "ROLE_USER"),
                null
        ));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
