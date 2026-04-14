package com.dealit.dealit.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.chat.dto.ChatMessageListResponse;
import com.dealit.dealit.domain.chat.dto.ChatRoomListItemResponse;
import com.dealit.dealit.domain.chat.dto.ChatRoomListResponse;
import com.dealit.dealit.domain.chat.dto.CreateChatRoomRequest;
import com.dealit.dealit.domain.chat.dto.CreateChatRoomResponse;
import com.dealit.dealit.domain.chat.dto.SendChatMessageRequest;
import com.dealit.dealit.domain.chat.entity.ChatMessageType;
import com.dealit.dealit.domain.chat.service.ChatService;
import com.dealit.dealit.domain.chat.service.ProductOwnershipPort;
import com.dealit.dealit.domain.chat.service.ProductSummaryPort;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatMessageIntegrationTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ProductOwnershipPort productOwnershipPort;

    @MockitoBean
    private ProductSummaryPort productSummaryPort;

    @Test
    @DisplayName("fallback 계약: room API에서 닉네임 공백은 User#id, null 필드는 null 유지")
    void fallbackForRoomApis() {
        Member seller = createMember("seller");
        Member buyer = createMember("buyer");
        String buyerFallbackNickname = "User#" + buyer.getMemberId();
        Long productId = 100L;

        setNicknameBlank(buyer.getMemberId());
        stubProduct(productId, seller.getMemberId(), null);

        CreateChatRoomResponse created = chatService.createChatRoom(
                new CreateChatRoomRequest(productId, buyer.getMemberId()),
                seller.getMemberId()
        );

        assertThat(created.participants())
                .extracting(CreateChatRoomResponse.ParticipantInfo::nickname)
                .contains(buyerFallbackNickname);
        assertThat(created.product().thumbnailUrl()).isNull();

        ChatRoomListResponse roomList = chatService.getChatRooms(seller.getMemberId(), 0, 20);
        assertThat(roomList.content()).hasSize(1);

        ChatRoomListItemResponse roomItem = roomList.content().get(0);
        assertThat(roomItem.opponent().nickname()).isEqualTo(buyerFallbackNickname);
        assertThat(roomItem.opponent().profileImageUrl()).isNull();
        assertThat(roomItem.lastMessage()).isNull();
        assertThat(roomItem.product().thumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("fallback 계약: sender soft-delete 이후에도 senderNickname snapshot 유지")
    void senderNicknameSnapshotRemainsAfterSoftDelete() {
        Member seller = createMember("seller");
        Member buyer = createMember("buyer");
        String buyerFallbackNickname = "User#" + buyer.getMemberId();
        Long productId = 200L;

        setNicknameBlank(buyer.getMemberId());
        stubProduct(productId, seller.getMemberId(), null);

        CreateChatRoomResponse created = chatService.createChatRoom(
                new CreateChatRoomRequest(productId, buyer.getMemberId()),
                seller.getMemberId()
        );

        chatService.sendMessage(
                created.roomId(),
                new SendChatMessageRequest(ChatMessageType.TEXT, "first message"),
                buyer.getMemberId()
        );

        Member buyerEntity = memberRepository.findById(buyer.getMemberId()).orElseThrow();
        buyerEntity.softDelete();
        memberRepository.saveAndFlush(buyerEntity);

        ChatMessageListResponse messageList =
                chatService.getChatMessages(created.roomId(), seller.getMemberId(), 0, 20);

        assertThat(messageList.content()).hasSize(1);
        assertThat(messageList.content().get(0).senderNickname()).isEqualTo(buyerFallbackNickname);
    }

    private void setNicknameBlank(Long memberId) {
        entityManager.createQuery("UPDATE Member m SET m.nickname = :nickname WHERE m.memberId = :memberId")
                .setParameter("nickname", "")
                .setParameter("memberId", memberId)
                .executeUpdate();
        entityManager.clear();
    }

    private Member createMember(String prefix) {
        String suffix = Long.toUnsignedString(System.nanoTime(), 36);
        String loginId = prefix + "_" + suffix;
        String email = loginId + "@t.com";

        return memberRepository.saveAndFlush(Member.create(
                loginId,
                "encoded-password",
                email,
                "010-0000-0000",
                prefix
        ));
    }

    private void stubProduct(Long productId, Long sellerId, String thumbnailUrl) {
        when(productOwnershipPort.getOwnerIdByProductId(productId)).thenReturn(sellerId);
        when(productSummaryPort.getSummaryByProductId(productId))
                .thenReturn(new ProductSummaryPort.ProductSummary(productId, "test-product", thumbnailUrl));
    }
}