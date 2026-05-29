package com.dealit.dealit.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.auction.AuctionPaymentStatus;
import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.entity.AuctionPayment;
import com.dealit.dealit.domain.auction.repository.AuctionPaymentRepository;
import com.dealit.dealit.domain.auction.service.AuctionNotificationService;
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
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.service.FcmNotificationService;
import com.dealit.dealit.domain.wallet.service.WalletService;
import com.dealit.dealit.domain.purchase.repository.PurchaseRepository;
import com.dealit.dealit.global.event.service.EventStreamService;
import com.dealit.dealit.global.service.ImageUrlService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
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
        AuctionPaymentRepository auctionPaymentRepository = mock(AuctionPaymentRepository.class);
        WalletService walletService = mock(WalletService.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);
        FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);
        AuctionNotificationService auctionNotificationService = mock(AuctionNotificationService.class);

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
                auctionPaymentRepository,
                walletService,
                purchaseRepository,
                eventStreamService,
                fcmNotificationService,
                auctionNotificationService,
                mock(ImageUrlService.class),
                Clock.systemUTC()
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
        AuctionPaymentRepository auctionPaymentRepository = mock(AuctionPaymentRepository.class);
        WalletService walletService = mock(WalletService.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);
        FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);
        AuctionNotificationService auctionNotificationService = mock(AuctionNotificationService.class);

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
                auctionPaymentRepository,
                walletService,
                purchaseRepository,
                eventStreamService,
                fcmNotificationService,
                auctionNotificationService,
                mock(ImageUrlService.class),
                Clock.systemUTC()
        );

        CreateChatRoomRequest request = new CreateChatRoomRequest(100L);

        CreateChatRoomResponse response = chatService.createChatRoom(request, 20L);

        assertThat(response.chatType()).isEqualTo(ChatType.GENERAL);
        assertThat(response.product().productId()).isEqualTo(100L);
        verify(chatRoomRepository, never()).save(org.mockito.ArgumentMatchers.any(ChatRoom.class));
    }

    @Test
    @DisplayName("낙찰자 경매 채팅방 응답에는 낙찰자 여부와 수령확정 버튼 상태를 포함한다")
    void getChatRoom_returnsWinnerAndReceiptAction_whenAuctionWinner() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ChatMessageReportRepository chatMessageReportRepository = mock(ChatMessageReportRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ProductOwnershipPort productOwnershipPort = mock(ProductOwnershipPort.class);
        ProductSummaryPort productSummaryPort = mock(ProductSummaryPort.class);
        AuctionPaymentRepository auctionPaymentRepository = mock(AuctionPaymentRepository.class);
        WalletService walletService = mock(WalletService.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);
        FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);
        AuctionNotificationService auctionNotificationService = mock(AuctionNotificationService.class);

        ChatRoom room = ChatRoom.create(10L, 20L, 100L, ChatType.AUCTION);
        ProductSummaryPort.ProductSummary product =
                new ProductSummaryPort.ProductSummary(100L, "auction-product", null, "AUCTION", 300L);
        AuctionPayment payment = mock(AuctionPayment.class);
        Member buyer = member("buyer");
        Member seller = member("seller");
        OffsetDateTime reservedAt = OffsetDateTime.parse("2026-05-10T10:00:00Z");
        OffsetDateTime shippedAt = OffsetDateTime.parse("2026-05-10T12:00:00Z");
        Auction auction = successfulAuction(20L);

        when(chatRoomRepository.findAccessibleRoom(1L, 20L)).thenReturn(Optional.of(room));
        when(productSummaryPort.getSummaryByProductId(100L)).thenReturn(product);
        when(memberRepository.findByMemberIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(buyer));
        when(memberRepository.findByMemberIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(seller));
        when(payment.getBidderId()).thenReturn(20L);
        when(payment.getStatus()).thenReturn(AuctionPaymentStatus.SHIPPED);
        when(payment.getReservedAt()).thenReturn(reservedAt);
        when(payment.getShippedAt()).thenReturn(shippedAt);
        when(payment.getAuction()).thenReturn(auction);
        when(auctionPaymentRepository
                .findLatestByAuctionAndParticipantsAndStatuses(
                        eq(300L),
                        eq(20L),
                        eq(10L),
                        org.mockito.ArgumentMatchers.anyCollection(),
                        org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)
                ))
                .thenReturn(List.of(payment));

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                auctionPaymentRepository,
                walletService,
                purchaseRepository,
                eventStreamService,
                fcmNotificationService,
                auctionNotificationService,
                mock(ImageUrlService.class),
                Clock.fixed(OffsetDateTime.parse("2026-05-10T13:00:00Z").toInstant(), java.time.ZoneOffset.UTC)
        );

        CreateChatRoomResponse response = chatService.getChatRoom(1L, 20L);

        assertThat(response.isWinner()).isTrue();
        assertThat(response.actionButtons().canConfirmReceipt()).isTrue();
        assertThat(response.actionButtons().confirmReceiptButtonType()).isEqualTo("CONFIRM_RECEIPT");
        assertThat(response.actionButtons().status()).isEqualTo("SHIPPED");
    }

    @Test
    @DisplayName("판매자 경매 채팅방 응답에는 발송 버튼 상태를 포함한다")
    void getChatRoom_returnsShipmentAction_whenAuctionSellerAndReserved() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ChatMessageReportRepository chatMessageReportRepository = mock(ChatMessageReportRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ProductOwnershipPort productOwnershipPort = mock(ProductOwnershipPort.class);
        ProductSummaryPort productSummaryPort = mock(ProductSummaryPort.class);
        AuctionPaymentRepository auctionPaymentRepository = mock(AuctionPaymentRepository.class);
        WalletService walletService = mock(WalletService.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);
        FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);
        AuctionNotificationService auctionNotificationService = mock(AuctionNotificationService.class);

        ChatRoom room = ChatRoom.create(10L, 20L, 100L, ChatType.AUCTION);
        ProductSummaryPort.ProductSummary product =
                new ProductSummaryPort.ProductSummary(100L, "auction-product", null, "AUCTION", 300L);
        AuctionPayment payment = mock(AuctionPayment.class);
        Member seller = member("seller");
        Member buyer = member("buyer");
        Auction auction = successfulAuction(20L);

        when(chatRoomRepository.findAccessibleRoom(1L, 10L)).thenReturn(Optional.of(room));
        when(productSummaryPort.getSummaryByProductId(100L)).thenReturn(product);
        when(memberRepository.findByMemberIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(seller));
        when(memberRepository.findByMemberIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(buyer));
        when(payment.getBidderId()).thenReturn(20L);
        when(payment.getStatus()).thenReturn(AuctionPaymentStatus.RESERVED);
        when(payment.getReservedAt()).thenReturn(OffsetDateTime.parse("2026-05-10T10:00:00Z"));
        when(payment.getAuction()).thenReturn(auction);
        when(auctionPaymentRepository
                .findLatestByAuctionAndParticipantsAndStatuses(
                        eq(300L),
                        eq(20L),
                        eq(10L),
                        org.mockito.ArgumentMatchers.anyCollection(),
                        org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)
                ))
                .thenReturn(List.of(payment));

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                auctionPaymentRepository,
                walletService,
                purchaseRepository,
                eventStreamService,
                fcmNotificationService,
                auctionNotificationService,
                mock(ImageUrlService.class),
                Clock.fixed(OffsetDateTime.parse("2026-05-10T13:00:00Z").toInstant(), java.time.ZoneOffset.UTC)
        );

        CreateChatRoomResponse response = chatService.getChatRoom(1L, 10L);

        assertThat(response.isWinner()).isFalse();
        assertThat(response.actionButtons().canShip()).isTrue();
        assertThat(response.actionButtons().shipButtonType()).isEqualTo("SHIP");
        assertThat(response.actionButtons().status()).isEqualTo("RESERVED");
    }

    @Test
    @DisplayName("기존 GENERAL 채팅방이어도 상품이 경매이고 결제 정보가 있으면 경매 액션을 포함한다")
    void getChatRoom_returnsAuctionActions_whenExistingRoomWasGeneral() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ChatMessageReportRepository chatMessageReportRepository = mock(ChatMessageReportRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ProductOwnershipPort productOwnershipPort = mock(ProductOwnershipPort.class);
        ProductSummaryPort productSummaryPort = mock(ProductSummaryPort.class);
        AuctionPaymentRepository auctionPaymentRepository = mock(AuctionPaymentRepository.class);
        WalletService walletService = mock(WalletService.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);
        FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);
        AuctionNotificationService auctionNotificationService = mock(AuctionNotificationService.class);

        ChatRoom room = ChatRoom.create(10L, 20L, 100L, ChatType.GENERAL);
        ProductSummaryPort.ProductSummary product =
                new ProductSummaryPort.ProductSummary(100L, "auction-product", null, "AUCTION", 300L);
        AuctionPayment payment = mock(AuctionPayment.class);
        Member seller = member("seller");
        Member buyer = member("buyer");
        Auction auction = successfulAuction(20L);

        when(chatRoomRepository.findAccessibleRoom(1L, 10L)).thenReturn(Optional.of(room));
        when(productSummaryPort.getSummaryByProductId(100L)).thenReturn(product);
        when(memberRepository.findByMemberIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(seller));
        when(memberRepository.findByMemberIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(buyer));
        when(payment.getBidderId()).thenReturn(20L);
        when(payment.getStatus()).thenReturn(AuctionPaymentStatus.RESERVED);
        when(payment.getReservedAt()).thenReturn(OffsetDateTime.parse("2026-05-10T10:00:00Z"));
        when(payment.getAuction()).thenReturn(auction);
        when(auctionPaymentRepository
                .findLatestByAuctionAndParticipantsAndStatuses(
                        eq(300L),
                        eq(20L),
                        eq(10L),
                        org.mockito.ArgumentMatchers.anyCollection(),
                        org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)
                ))
                .thenReturn(List.of(payment));

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                auctionPaymentRepository,
                walletService,
                purchaseRepository,
                eventStreamService,
                fcmNotificationService,
                auctionNotificationService,
                mock(ImageUrlService.class),
                Clock.fixed(OffsetDateTime.parse("2026-05-10T13:00:00Z").toInstant(), java.time.ZoneOffset.UTC)
        );

        CreateChatRoomResponse response = chatService.getChatRoom(1L, 10L);

        assertThat(response.chatType()).isEqualTo(ChatType.AUCTION);
        assertThat(response.actionButtons().canShip()).isTrue();
        assertThat(response.actionButtons().shipButtonType()).isEqualTo("SHIP");
    }

    @Test
    @DisplayName("낙찰 완료 전 경매 채팅방에는 거래 진행 버튼을 노출하지 않는다")
    void getChatRoom_hidesAuctionActions_beforeSuccessfulBid() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ChatMessageReportRepository chatMessageReportRepository = mock(ChatMessageReportRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ProductOwnershipPort productOwnershipPort = mock(ProductOwnershipPort.class);
        ProductSummaryPort productSummaryPort = mock(ProductSummaryPort.class);
        AuctionPaymentRepository auctionPaymentRepository = mock(AuctionPaymentRepository.class);
        WalletService walletService = mock(WalletService.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);
        FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);
        AuctionNotificationService auctionNotificationService = mock(AuctionNotificationService.class);

        ChatRoom room = ChatRoom.create(10L, 20L, 100L, ChatType.AUCTION);
        ProductSummaryPort.ProductSummary product =
                new ProductSummaryPort.ProductSummary(100L, "auction-product", null, "AUCTION", 300L);
        AuctionPayment payment = mock(AuctionPayment.class);
        Member seller = member("seller");
        Member buyer = member("buyer");
        Auction ongoingAuction = mock(Auction.class);

        when(chatRoomRepository.findAccessibleRoom(1L, 10L)).thenReturn(Optional.of(room));
        when(productSummaryPort.getSummaryByProductId(100L)).thenReturn(product);
        when(memberRepository.findByMemberIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(seller));
        when(memberRepository.findByMemberIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(buyer));
        when(payment.getBidderId()).thenReturn(20L);
        when(payment.getStatus()).thenReturn(AuctionPaymentStatus.RESERVED);
        when(payment.getAuction()).thenReturn(ongoingAuction);
        when(ongoingAuction.getStatus()).thenReturn(AuctionStatus.ONGOING);
        when(auctionPaymentRepository
                .findLatestByAuctionAndParticipantsAndStatuses(
                        eq(300L),
                        eq(20L),
                        eq(10L),
                        org.mockito.ArgumentMatchers.anyCollection(),
                        org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)
                ))
                .thenReturn(List.of(payment));

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                auctionPaymentRepository,
                walletService,
                purchaseRepository,
                eventStreamService,
                fcmNotificationService,
                auctionNotificationService,
                mock(ImageUrlService.class),
                Clock.fixed(OffsetDateTime.parse("2026-05-10T13:00:00Z").toInstant(), java.time.ZoneOffset.UTC)
        );

        CreateChatRoomResponse response = chatService.getChatRoom(1L, 10L);

        assertThat(response.isWinner()).isFalse();
        assertThat(response.actionButtons().canShip()).isFalse();
        assertThat(response.actionButtons().shipButtonType()).isNull();
        assertThat(response.actionButtons().canConfirmReceipt()).isFalse();
        assertThat(response.actionButtons().confirmReceiptButtonType()).isNull();
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
        AuctionPaymentRepository auctionPaymentRepository = mock(AuctionPaymentRepository.class);
        WalletService walletService = mock(WalletService.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        EventStreamService eventStreamService = mock(EventStreamService.class);
        FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);
        AuctionNotificationService auctionNotificationService = mock(AuctionNotificationService.class);

        when(productOwnershipPort.getOwnerIdByProductId(404L))
                .thenThrow(new ProductNotFoundException("유효한 상품을 찾을 수 없습니다. productId=404"));

        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                chatMessageReportRepository,
                memberRepository,
                productOwnershipPort,
                productSummaryPort,
                auctionPaymentRepository,
                walletService,
                purchaseRepository,
                eventStreamService,
                fcmNotificationService,
                auctionNotificationService,
                mock(ImageUrlService.class),
                Clock.systemUTC()
        );

        CreateChatRoomRequest request = new CreateChatRoomRequest(404L);

        assertThatThrownBy(() -> chatService.createChatRoom(request, 1L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("productId=404");
    }

    private Member member(String nickname) {
        Member member = mock(Member.class);
        when(member.getNickname()).thenReturn(nickname);
        return member;
    }

    private Auction successfulAuction(Long winnerId) {
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.SUCCESSFUL_BID);
        when(auction.getWinnerId()).thenReturn(winnerId);
        return auction;
    }
}
