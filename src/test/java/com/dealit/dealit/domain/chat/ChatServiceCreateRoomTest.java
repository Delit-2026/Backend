package com.dealit.dealit.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.chat.dto.CreateChatRoomRequest;
import com.dealit.dealit.domain.chat.dto.CreateChatRoomResponse;
import com.dealit.dealit.domain.chat.entity.ChatRoom;
import com.dealit.dealit.domain.chat.entity.ChatType;
import com.dealit.dealit.domain.chat.exception.ProductNotFoundException;
import com.dealit.dealit.domain.chat.repository.ChatMessageReportRepository;
import com.dealit.dealit.domain.chat.repository.ChatMessageRepository;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.domain.chat.service.ChatService;
import com.dealit.dealit.domain.chat.service.ProductOwnershipPort;
import com.dealit.dealit.domain.chat.service.ProductSummaryPort;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.service.FcmNotificationService;
import com.dealit.dealit.global.event.service.EventStreamService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatServiceCreateRoomTest {

    @Test
    @DisplayName("본인 상품에는 채팅방 생성 불가")
    void createChatRoom_fail_whenSelfChat() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ChatMessageReportRepository chatMessageReportRepository = mock(ChatMessageReportRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ProductOwnershipPort productOwnershipPort = mock(ProductOwnershipPort.class);
        ProductSummaryPort productSummaryPort = mock(ProductSummaryPort.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);
        FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);

        when(chatRoomRepository.findBySellerIdAndBuyerIdAndProductIdAndDeletedAtIsNull(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(productOwnershipPort.getOwnerIdByProductId(100L)).thenReturn(1L);

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                eventStreamService,
                fcmNotificationService
        );

        CreateChatRoomRequest request = new CreateChatRoomRequest(100L);

        assertThatThrownBy(() -> chatService.createChatRoom(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인 상품에는 채팅방을 생성할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 같은 상품 채팅방이 있으면 기존 방을 반환한다")
    void createChatRoom_returnsExistingRoom_whenDuplicated() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ChatMessageReportRepository chatMessageReportRepository = mock(ChatMessageReportRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ProductOwnershipPort productOwnershipPort = mock(ProductOwnershipPort.class);
        ProductSummaryPort productSummaryPort = mock(ProductSummaryPort.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);
        FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);

        ChatRoom existingRoom = ChatRoom.create(10L, 20L, 100L, ChatType.GENERAL);
        when(productOwnershipPort.getOwnerIdByProductId(100L)).thenReturn(10L);
        when(chatRoomRepository.findBySellerIdAndBuyerIdAndProductIdAndDeletedAtIsNull(10L, 20L, 100L))
                .thenReturn(Optional.of(existingRoom));
        when(productSummaryPort.getSummaryByProductId(100L))
                .thenReturn(new ProductSummaryPort.ProductSummary(100L, "test-product", null));

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                eventStreamService,
                fcmNotificationService
        );

        CreateChatRoomRequest request = new CreateChatRoomRequest(100L);

        CreateChatRoomResponse response = chatService.createChatRoom(request, 20L);

        assertThat(response.chatType()).isEqualTo(ChatType.GENERAL);
        assertThat(response.product().productId()).isEqualTo(100L);
        verify(chatRoomRepository, never()).save(org.mockito.ArgumentMatchers.any(ChatRoom.class));
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
        FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);

        when(productOwnershipPort.getOwnerIdByProductId(404L))
                .thenThrow(new ProductNotFoundException("유효한 상품을 찾을 수 없습니다. productId=404"));

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                eventStreamService,
                fcmNotificationService
        );

        CreateChatRoomRequest request = new CreateChatRoomRequest(404L);

        assertThatThrownBy(() -> chatService.createChatRoom(request, 1L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("productId=404");
    }
}
