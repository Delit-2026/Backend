package com.dealit.dealit.domain.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.chat.dto.CreateChatRoomRequest;
import com.dealit.dealit.domain.chat.repository.ChatMessageReportRepository;
import com.dealit.dealit.domain.chat.repository.ChatMessageRepository;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.domain.chat.service.ChatService;
import com.dealit.dealit.domain.chat.service.ProductOwnershipPort;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatServiceCreateRoomTest {

    @Test
    @DisplayName("자기 자신과는 채팅방 생성 불가")
    void createChatRoom_fail_whenSelfChat() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ChatMessageReportRepository chatMessageReportRepository = mock(ChatMessageReportRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ProductOwnershipPort productOwnershipPort = mock(ProductOwnershipPort.class);

        when(chatRoomRepository.findBySellerIdAndBuyerIdAndProductIdAndDeletedAtIsNull(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort
        );

        CreateChatRoomRequest request = new CreateChatRoomRequest(100L, 1L);

        assertThatThrownBy(() -> chatService.createChatRoom(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자기 자신과는 채팅방을 생성할 수 없습니다.");
    }

    @Test
    @DisplayName("상품 소유자가 참여자가 아니면 채팅방 생성 불가")
    void createChatRoom_fail_whenOwnerIsNotParticipant() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ChatMessageReportRepository chatMessageReportRepository = mock(ChatMessageReportRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ProductOwnershipPort productOwnershipPort = mock(ProductOwnershipPort.class);

        when(productOwnershipPort.getOwnerIdByProductId(100L)).thenReturn(999L); // currentUser(1), receiver(2) 아님

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort
        );

        CreateChatRoomRequest request = new CreateChatRoomRequest(100L, 2L);

        assertThatThrownBy(() -> chatService.createChatRoom(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품 소유자는 채팅 참여자 중 한 명이어야 합니다.");
    }
}