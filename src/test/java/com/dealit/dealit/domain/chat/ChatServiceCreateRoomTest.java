package com.dealit.dealit.domain.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.chat.dto.CreateChatRoomRequest;
import com.dealit.dealit.domain.chat.exception.ProductNotFoundException;
import com.dealit.dealit.domain.chat.repository.ChatMessageReportRepository;
import com.dealit.dealit.domain.chat.repository.ChatMessageRepository;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.domain.chat.service.ChatService;
import com.dealit.dealit.domain.chat.service.ProductOwnershipPort;
import com.dealit.dealit.domain.chat.service.ProductSummaryPort;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.global.event.service.EventStreamService;
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
        ProductSummaryPort productSummaryPort = mock(ProductSummaryPort.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);

        when(chatRoomRepository.findBySellerIdAndBuyerIdAndProductIdAndDeletedAtIsNull(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                eventStreamService
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
        ProductSummaryPort productSummaryPort = mock(ProductSummaryPort.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);

        when(productOwnershipPort.getOwnerIdByProductId(100L)).thenReturn(999L);

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                eventStreamService
        );

        CreateChatRoomRequest request = new CreateChatRoomRequest(100L, 2L);

        assertThatThrownBy(() -> chatService.createChatRoom(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품 소유자는 채팅 참여자 중 한 명이어야 합니다.");
    }

    @Test
    @DisplayName("상품이 없으면 ProductNotFoundException을 전파한다 (strict)")
    void createChatRoom_fail_whenProductNotFound() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ChatMessageReportRepository chatMessageReportRepository = mock(ChatMessageReportRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ProductOwnershipPort productOwnershipPort = mock(ProductOwnershipPort.class);
        ProductSummaryPort productSummaryPort = mock(ProductSummaryPort.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);

        when(productOwnershipPort.getOwnerIdByProductId(404L))
                .thenThrow(new ProductNotFoundException("유효한 상품을 찾을 수 없습니다. productId=404"));

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                eventStreamService
        );

        CreateChatRoomRequest request = new CreateChatRoomRequest(404L, 2L);

        assertThatThrownBy(() -> chatService.createChatRoom(request, 1L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("productId=404");
    }
}
