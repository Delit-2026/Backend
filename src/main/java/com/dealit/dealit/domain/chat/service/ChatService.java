package com.dealit.dealit.domain.chat.service;

import com.dealit.dealit.domain.auction.AuctionPaymentStatus;
import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.AuctionPayment;
import com.dealit.dealit.domain.auction.repository.AuctionPaymentRepository;
import com.dealit.dealit.domain.auction.service.AuctionNotificationService;
import com.dealit.dealit.domain.chat.dto.ChatMessageListResponse;
import com.dealit.dealit.domain.chat.dto.ChatMessageResponse;
import com.dealit.dealit.domain.chat.dto.ChatRoomDetailResponse;
import com.dealit.dealit.domain.chat.dto.ChatRoomListItemResponse;
import com.dealit.dealit.domain.chat.dto.ChatRoomListResponse;
import com.dealit.dealit.domain.chat.dto.ChatRoomUpdatedEvent;
import com.dealit.dealit.domain.chat.dto.ChatUnreadCountUpdatedEvent;
import com.dealit.dealit.domain.chat.dto.CreateChatRoomRequest;
import com.dealit.dealit.domain.chat.dto.CreateChatRoomResponse;
import com.dealit.dealit.domain.chat.dto.MarkChatRoomAsReadResponse;
import com.dealit.dealit.domain.chat.dto.ReportChatMessageRequest;
import com.dealit.dealit.domain.chat.dto.ReportChatMessageResponse;
import com.dealit.dealit.domain.chat.dto.RoomUnreadCountResponse;
import com.dealit.dealit.domain.chat.dto.SendChatMessageRequest;
import com.dealit.dealit.domain.chat.dto.SendChatMessageResponse;
import com.dealit.dealit.domain.chat.dto.UnreadCountResponse;
import com.dealit.dealit.domain.chat.entity.ChatMessage;
import com.dealit.dealit.domain.chat.entity.ChatMessageReport;
import com.dealit.dealit.domain.chat.entity.ChatRoom;
import com.dealit.dealit.domain.chat.entity.ChatType;
import com.dealit.dealit.domain.chat.exception.ChatForbiddenException;
import com.dealit.dealit.domain.chat.exception.ChatMessageNotFoundException;
import com.dealit.dealit.domain.chat.exception.ChatRoomNotFoundException;
import com.dealit.dealit.domain.chat.exception.DuplicateChatReportException;
import com.dealit.dealit.domain.chat.exception.ProductNotFoundException;
import com.dealit.dealit.domain.chat.repository.ChatMessageReportRepository;
import com.dealit.dealit.domain.chat.repository.ChatMessageRepository;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.service.FcmNotificationService;
import com.dealit.dealit.domain.wallet.service.WalletService;
import com.dealit.dealit.domain.purchase.entity.Purchase;
import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import com.dealit.dealit.domain.purchase.repository.PurchaseRepository;
import com.dealit.dealit.global.event.service.EventStreamService;
import com.dealit.dealit.global.service.ImageUrlService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageReportRepository chatMessageReportRepository;
    private final MemberRepository memberRepository;
    private final ProductOwnershipPort productOwnershipPort;
    private final ProductSummaryPort productSummaryPort;
    private final AuctionPaymentRepository auctionPaymentRepository;
    private final WalletService walletService;
    private final PurchaseRepository purchaseRepository;
    private final EventStreamService eventStreamService;
    private final FcmNotificationService fcmNotificationService;
    private final AuctionNotificationService auctionNotificationService;
    private final ImageUrlService imageUrlService;
    private final Clock clock;

    public CreateChatRoomResponse createChatRoom(CreateChatRoomRequest request, Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }
        if (request.productId() == null) {
            throw new IllegalArgumentException("productId는 필수입니다.");
        }

        Long sellerId = resolveSellerIdFromProduct(request.productId());
        if (sellerId.equals(currentUserId)) {
            throw new IllegalArgumentException("본인 상품에는 채팅방을 생성할 수 없습니다.");
        }
        Long buyerId = currentUserId;

        Optional<ChatRoom> existingRoom = chatRoomRepository
                .findBySellerIdAndBuyerIdAndProductIdAndDeletedAtIsNull(
                        sellerId, buyerId, request.productId()
                );
        if (existingRoom.isPresent()) {
            return buildCreateRoomResponse(existingRoom.get(), currentUserId, sellerId, buyerId, existingRoom.get().getCreatedAt());
        }

        ProductSummaryPort.ProductSummary product = resolveProductSummaryOrFallback(request.productId());
        ChatRoom saved = chatRoomRepository.save(
                ChatRoom.create(sellerId, buyerId, request.productId(), resolveChatType(product))
        );
        LocalDateTime now = LocalDateTime.now(clock);

        CreateChatRoomResponse response = buildCreateRoomResponse(saved, currentUserId, sellerId, buyerId, now);

        publishRoomAndUnreadUpdates(saved, sellerId, buyerId, now);
        return response;
    }

    private CreateChatRoomResponse buildCreateRoomResponse(
            ChatRoom room,
            Long currentUserId,
            Long sellerId,
            Long buyerId,
            LocalDateTime createdAt
    ) {
        Long opponentId = sellerId.equals(currentUserId) ? buyerId : sellerId;
        MemberSnapshot currentUser = resolveMemberSnapshot(currentUserId);
        MemberSnapshot opponent = resolveMemberSnapshot(opponentId);
        ProductSummaryPort.ProductSummary product = resolveProductSummaryOrFallback(room.getProductId());
        Optional<AuctionPayment> auctionPayment = resolveAuctionPayment(room, product);

        return new CreateChatRoomResponse(
                room.getRoomId(),
                resolveChatType(product),
                new CreateChatRoomResponse.ProductInfo(
                        product.productId(),
                        product.name(),
                        product.thumbnailUrl(),
                        product.saleType(),
                        product.auctionId(),
                        "ACTIVE"
                ),
                List.of(
                        new CreateChatRoomResponse.ParticipantInfo(
                                currentUserId,
                                currentUser.nickname(),
                                sellerId.equals(currentUserId) ? "SELLER" : "BUYER"
                        ),
                        new CreateChatRoomResponse.ParticipantInfo(
                                opponentId,
                                opponent.nickname(),
                                sellerId.equals(opponentId) ? "SELLER" : "BUYER"
                        )
                ),
                isWinner(currentUserId, auctionPayment),
                buildActionButtons(room, currentUserId, auctionPayment),
                createdAt
        );
    }

    public CreateChatRoomResponse markShipment(Long roomId, Long currentUserId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        ChatRoom room = chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));
        if (!room.getSellerId().equals(currentUserId)) {
            throw new ChatForbiddenException("판매자만 발송 처리를 할 수 있습니다.");
        }

        ProductSummaryPort.ProductSummary product = resolveProductSummaryOrFallback(room.getProductId());
        AuctionPayment payment = resolveAuctionPayment(room, product, true)
                .orElseThrow(() -> new IllegalArgumentException("발송 처리할 경매 결제를 찾을 수 없습니다."));
        if (!isAuctionTradeReady(payment)) {
            throw new IllegalArgumentException("낙찰 완료 후 발송 처리할 수 있습니다.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (payment.getReservedAt().plusDays(3).isBefore(now)) {
            throw new IllegalArgumentException("발송 처리 기한이 지나 자동 환불 대상입니다.");
        }
        if (!payment.markShipped(now)) {
            throw new IllegalArgumentException("현재 상태에서는 발송 처리를 할 수 없습니다.");
        }
        auctionNotificationService.notifyAuctionShipped(payment.getAuction(), payment.getBidderId(), room.getRoomId());
        syncAuctionPurchaseShipped(payment.getPurchaseId());

        publishRoomAndUnreadUpdates(room, room.getSellerId(), room.getBuyerId(), LocalDateTime.now(clock));
        return buildCreateRoomResponse(room, currentUserId, room.getSellerId(), room.getBuyerId(), room.getCreatedAt());
    }

    public CreateChatRoomResponse confirmReceipt(Long roomId, Long currentUserId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        ChatRoom room = chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));
        if (!room.getBuyerId().equals(currentUserId)) {
            throw new ChatForbiddenException("구매자만 수령확정을 할 수 있습니다.");
        }

        ProductSummaryPort.ProductSummary product = resolveProductSummaryOrFallback(room.getProductId());
        AuctionPayment payment = resolveAuctionPayment(room, product, true)
                .orElseThrow(() -> new IllegalArgumentException("수령확정할 경매 결제를 찾을 수 없습니다."));
        if (!isAuctionTradeReady(payment)) {
            throw new IllegalArgumentException("낙찰 완료 후 수령확정할 수 있습니다.");
        }
        if (!payment.confirmReceived(OffsetDateTime.now(clock))) {
            throw new IllegalArgumentException("현재 상태에서는 수령확정을 할 수 없습니다.");
        }
        walletService.settleAuctionPayment(
                payment.getSellerId(),
                payment.getAmount(),
                payment.getAuction().getAuctionId()
        );
        auctionNotificationService.notifyAuctionReceived(payment.getAuction(), payment.getSellerId(), room.getRoomId());
        auctionNotificationService.notifyReviewRequest(payment.getAuction(), currentUserId);
        syncAuctionPurchaseCompleted(payment.getPurchaseId());

        publishRoomAndUnreadUpdates(room, room.getSellerId(), room.getBuyerId(), LocalDateTime.now(clock));
        return buildCreateRoomResponse(room, currentUserId, room.getSellerId(), room.getBuyerId(), room.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public CreateChatRoomResponse getChatRoom(Long roomId, Long currentUserId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        ChatRoom room = chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));
        return buildCreateRoomResponse(room, currentUserId, room.getSellerId(), room.getBuyerId(), room.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ChatRoomListResponse getChatRooms(Long currentUserId, int page, int size) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<ChatRoom> roomPage = chatRoomRepository.findAllByParticipant(currentUserId, pageable);

        List<ChatRoomListItemResponse> content = roomPage.getContent().stream()
                .map(room -> buildRoomListItem(room, currentUserId))
                .toList();

        return new ChatRoomListResponse(
                content,
                roomPage.getNumber(),
                roomPage.getSize(),
                roomPage.getTotalElements(),
                roomPage.getTotalPages(),
                roomPage.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getChatRoomDetail(Long roomId, Long currentUserId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        ChatRoom room = chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));

        String currentUserRole = room.getSellerId().equals(currentUserId) ? "SELLER" : "BUYER";
        Long opponentId = room.getOpponentId(currentUserId);
        MemberSnapshot opponent = resolveMemberSnapshot(opponentId);
        ProductSummaryPort.ProductSummary product = resolveProductSummaryOrFallback(room.getProductId());

        Purchase purchase = null;
        if (room.getChatType() == ChatType.GENERAL) {
            purchase = purchaseRepository
                    .findFirstByProductIdAndSellerIdAndBuyerIdOrderByPurchaseIdDesc(
                            room.getProductId(),
                            room.getSellerId(),
                            room.getBuyerId()
                    )
                    .orElse(null);
        }
        PurchaseStatus tradeStatus = purchase == null ? null : purchase.getStatus();
        ChatRoomDetailResponse.ActionButton actionButton = room.getChatType() == ChatType.GENERAL
                ? buildActionButton(currentUserRole, tradeStatus)
                : null;

        return new ChatRoomDetailResponse(
                room.getRoomId(),
                purchase == null ? null : purchase.getPurchaseId(),
                room.getChatType(),
                currentUserRole,
                tradeStatus,
                new ChatRoomDetailResponse.ProductInfo(
                        product.productId(),
                        product.name(),
                        product.thumbnailUrl()
                ),
                new ChatRoomDetailResponse.OpponentInfo(
                        opponentId,
                        opponent.nickname()
                ),
                actionButton
        );
    }

    @Transactional(readOnly = true)
    public ChatMessageListResponse getChatMessages(Long roomId, Long currentUserId, int page, int size) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<ChatMessage> messagePage = chatMessageRepository.findByRoomId(roomId, pageable);

        List<ChatMessageResponse> content = messagePage.getContent().stream()
                .map(message -> new ChatMessageResponse(
                        message.getMessageId(),
                        message.getSenderId(),
                        message.getSenderNickname(),
                        message.getMessageType(),
                        message.getContent(),
                        message.isRead(),
                        message.getSentAt(),
                        message.getMessageType().name().equals("SYSTEM")
                                ? "SYSTEM"
                                : message.getSenderId().equals(currentUserId) ? "ME" : "OTHER"
                ))
                .toList();

        return new ChatMessageListResponse(
                content,
                messagePage.getNumber(),
                messagePage.getSize(),
                messagePage.getTotalElements(),
                messagePage.getTotalPages(),
                messagePage.hasNext()
        );
    }

    public SendChatMessageResponse sendMessage(Long roomId, SendChatMessageRequest request, Long currentUserId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }
        if (request == null) {
            throw new IllegalArgumentException("요청 본문이 없습니다.");
        }

        ChatRoom room = chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));

        MemberSnapshot sender = resolveMemberSnapshot(currentUserId);

        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.create(
                        room.getRoomId(),
                        currentUserId,
                        sender.nickname(),
                        request.messageType(),
                        request.content()
                )
        );

        SendChatMessageResponse response = new SendChatMessageResponse(
                saved.getMessageId(),
                saved.getRoomId(),
                saved.getSenderId(),
                saved.getSenderNickname(),
                saved.getMessageType(),
                saved.getContent(),
                saved.isRead(),
                saved.getSentAt()
        );

        publishRoomAndUnreadUpdates(room, room.getSellerId(), room.getBuyerId(), saved.getSentAt());
        sendChatPushNotification(room, currentUserId, sender, saved);
        return response;
    }

    public MarkChatRoomAsReadResponse markAsRead(Long roomId, Long currentUserId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        ChatRoom room = chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));

        chatMessageRepository.markAllAsRead(roomId, currentUserId);

        long unreadAfter = chatMessageRepository.countUnreadByRoomId(roomId, currentUserId);
        int unreadCountAfter = unreadAfter > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) unreadAfter;
        LocalDateTime now = LocalDateTime.now(clock);

        MarkChatRoomAsReadResponse response = new MarkChatRoomAsReadResponse(
                roomId,
                "채팅방 메시지가 모두 읽음 처리되었습니다.",
                unreadCountAfter,
                now
        );

        publishRoomUpdate(room, currentUserId, now);
        publishUnreadCountUpdate(currentUserId, now);
        return response;
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        long count = chatMessageRepository.countTotalUnreadForUser(currentUserId);
        int totalUnread = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;

        return new UnreadCountResponse(
                totalUnread,
                LocalDateTime.now(clock)
        );
    }

    @Transactional(readOnly = true)
    public RoomUnreadCountResponse getRoomUnreadCount(Long roomId, Long currentUserId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));

        long count = chatMessageRepository.countUnreadByRoomId(roomId, currentUserId);
        int unreadCount = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;

        return new RoomUnreadCountResponse(
                roomId,
                unreadCount,
                LocalDateTime.now(clock)
        );
    }

    public ReportChatMessageResponse reportMessage(Long messageId, ReportChatMessageRequest request, Long currentUserId) {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }
        if (request == null) {
            throw new IllegalArgumentException("요청 본문이 없습니다.");
        }

        ChatMessage message = chatMessageRepository.findByMessageIdAndDeletedAtIsNull(messageId)
                .orElseThrow(() -> new ChatMessageNotFoundException("신고할 메시지를 찾을 수 없습니다."));

        boolean alreadyReported = chatMessageReportRepository.existsActiveReport(messageId, currentUserId);
        if (alreadyReported) {
            throw new DuplicateChatReportException("이미 신고한 메시지입니다.");
        }

        ChatMessageReport saved = chatMessageReportRepository.save(
                ChatMessageReport.create(
                        message.getMessageId(),
                        currentUserId,
                        request.reason()
                )
        );

        return new ReportChatMessageResponse(
                saved.getReportId(),
                saved.getMessageId(),
                saved.getReason(),
                LocalDateTime.now(clock)
        );
    }

    public void deleteMessage(Long messageId, Long currentUserId) {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        ChatMessage message = chatMessageRepository.findByMessageIdAndDeletedAtIsNull(messageId)
                .orElseThrow(() -> new ChatMessageNotFoundException("삭제할 메시지를 찾을 수 없습니다."));

        if (!message.getSenderId().equals(currentUserId)) {
            throw new ChatForbiddenException("본인이 보낸 메시지만 삭제할 수 있습니다.");
        }

        ChatRoom room = chatRoomRepository.findById(message.getRoomId())
                .orElseThrow(ChatRoomNotFoundException::new);

        message.softDelete();
        publishRoomAndUnreadUpdates(room, room.getSellerId(), room.getBuyerId(), LocalDateTime.now(clock));
    }

    private void publishRoomAndUnreadUpdates(ChatRoom room, Long sellerId, Long buyerId, LocalDateTime emittedAt) {
        publishRoomUpdate(room, sellerId, emittedAt);
        publishRoomUpdate(room, buyerId, emittedAt);
        publishUnreadCountUpdate(sellerId, emittedAt);
        publishUnreadCountUpdate(buyerId, emittedAt);
    }

    private void publishRoomUpdate(ChatRoom room, Long userId, LocalDateTime emittedAt) {
        eventStreamService.publishRoomUpdated(
                userId,
                ChatRoomUpdatedEvent.of(buildRoomListItem(room, userId), emittedAt)
        );
    }

    private void publishUnreadCountUpdate(Long userId, LocalDateTime emittedAt) {
        long count = chatMessageRepository.countTotalUnreadForUser(userId);
        int totalUnread = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        eventStreamService.publishUnreadCountUpdated(
                userId,
                ChatUnreadCountUpdatedEvent.of(totalUnread, emittedAt)
        );
    }

    private void sendChatPushNotification(
            ChatRoom room,
            Long senderId,
            MemberSnapshot sender,
            ChatMessage message
    ) {
        Long recipientId = room.getOpponentId(senderId);
        String body = switch (message.getMessageType()) {
            case IMAGE -> "사진을 보냈습니다.";
            case SYSTEM -> "채팅방에 새 알림이 있습니다.";
            case TEXT, TALK -> message.getContent();
        };

        try {
            int sentCount = fcmNotificationService.sendToMember(
                    recipientId,
                    sender.nickname(),
                    body,
                    Map.of(
                            "type", "CHAT_MESSAGE",
                            "roomId", String.valueOf(room.getRoomId()),
                            "messageId", String.valueOf(message.getMessageId()),
                            "senderId", String.valueOf(senderId),
                            "targetUrl", "/chats/" + room.getRoomId()
                    )
            );
            log.debug("Sent chat push notification. roomId={}, recipientId={}, sentCount={}",
                    room.getRoomId(), recipientId, sentCount);
        } catch (RuntimeException exception) {
            log.warn("Failed to send chat push notification. roomId={}, recipientId={}",
                    room.getRoomId(), recipientId, exception);
        }
    }

    private ChatRoomListItemResponse buildRoomListItem(ChatRoom room, Long currentUserId) {
        Long opponentId = room.getOpponentId(currentUserId);
        MemberSnapshot opponent = resolveMemberSnapshot(opponentId);
        ProductSummaryPort.ProductSummary product = resolveProductSummaryOrFallback(room.getProductId());
        ChatMessage lastMessage = chatMessageRepository.findLatestMessage(room.getRoomId()).orElse(null);

        long unreadCountLong = chatMessageRepository.countUnreadByRoomId(room.getRoomId(), currentUserId);
        int unreadCount = unreadCountLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) unreadCountLong;

        ChatRoomListItemResponse.LastMessageInfo lastMessageInfo = (lastMessage == null)
                ? null
                : new ChatRoomListItemResponse.LastMessageInfo(
                lastMessage.getMessageId(),
                lastMessage.getMessageType().name(),
                lastMessage.getContent(),
                lastMessage.getSentAt()
        );

        LocalDateTime updatedAt = lastMessage != null
                ? lastMessage.getSentAt()
                : room.getUpdatedAt() != null ? room.getUpdatedAt() : LocalDateTime.now(clock);

        return new ChatRoomListItemResponse(
                room.getRoomId(),
                new ChatRoomListItemResponse.OpponentInfo(
                        opponentId,
                        opponent.nickname(),
                        opponent.profileImageUrl()
                ),
                new ChatRoomListItemResponse.ProductInfo(
                        product.productId(),
                        product.name(),
                        product.thumbnailUrl(),
                        product.saleType(),
                        product.auctionId()
                ),
                resolveChatType(product),
                lastMessageInfo,
                unreadCount,
                updatedAt
        );
    }

    private ChatRoomDetailResponse.ActionButton buildActionButton(String currentUserRole, PurchaseStatus tradeStatus) {
        if ("SELLER".equals(currentUserRole)) {
            boolean enabled = tradeStatus == PurchaseStatus.PAID;
            return new ChatRoomDetailResponse.ActionButton(
                    "물건을 보냈어요",
                    enabled,
                    "SELLER_CONFIRM",
                    enabled ? null : resolveSellerDisabledReason(tradeStatus)
            );
        }

        boolean enabled = tradeStatus == PurchaseStatus.SHIPPED;
        return new ChatRoomDetailResponse.ActionButton(
                "물건을 받았어요",
                enabled,
                "BUYER_CONFIRM",
                enabled ? null : resolveBuyerDisabledReason(tradeStatus)
        );
    }

    private String resolveSellerDisabledReason(PurchaseStatus tradeStatus) {
        if (tradeStatus == null) {
            return "PURCHASE_NOT_FOUND";
        }
        return switch (tradeStatus) {
            case PAID -> null;
            case SHIPPED -> "ALREADY_SHIPPED";
            case COMPLETED, CANCELED -> "TRADE_ALREADY_CLOSED";
        };
    }

    private String resolveBuyerDisabledReason(PurchaseStatus tradeStatus) {
        if (tradeStatus == null) {
            return "PURCHASE_NOT_FOUND";
        }
        return switch (tradeStatus) {
            case PAID -> "SELLER_NOT_SHIPPED_YET";
            case SHIPPED -> null;
            case COMPLETED, CANCELED -> "TRADE_ALREADY_CLOSED";
        };
    }

    private Long resolveSellerIdFromProduct(Long productId) {
        return productOwnershipPort.getOwnerIdByProductId(productId);
    }

    private ProductSummaryPort.ProductSummary resolveProductSummaryOrFallback(Long productId) {
        try {
            return productSummaryPort.getSummaryByProductId(productId);
        } catch (ProductNotFoundException exception) {
            return new ProductSummaryPort.ProductSummary(productId, null, null, "REGULAR", null);
        }
    }

    private ChatType resolveChatType(ProductSummaryPort.ProductSummary product) {
        return "AUCTION".equals(product.saleType()) ? ChatType.AUCTION : ChatType.GENERAL;
    }

    private CreateChatRoomResponse.ActionButtons buildActionButtons(
            ChatRoom room,
            Long currentUserId,
            Optional<AuctionPayment> optionalPayment
    ) {
        if (optionalPayment.isEmpty()) {
            return inactiveActionButtons(null, null, null);
        }

        AuctionPayment payment = optionalPayment.get();
        if (!isAuctionTradeReady(payment)) {
            return inactiveActionButtons(payment.getStatus().name(), null, null);
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime shipDeadline = payment.getReservedAt().plusDays(3);
        OffsetDateTime receiptDeadline = payment.getShippedAt() == null ? null : payment.getShippedAt().plusDays(7);
        boolean seller = room.getSellerId().equals(currentUserId);
        boolean buyer = room.getBuyerId().equals(currentUserId);
        boolean showShipButton = seller && payment.getStatus() == AuctionPaymentStatus.RESERVED;
        boolean showConfirmReceiptButton = buyer
                && (payment.getStatus() == AuctionPaymentStatus.RESERVED
                || payment.getStatus() == AuctionPaymentStatus.SHIPPED);
        boolean canShip = seller
                && payment.getStatus() == AuctionPaymentStatus.RESERVED
                && !shipDeadline.isBefore(now);
        boolean canConfirmReceipt = buyer && payment.getStatus() == AuctionPaymentStatus.SHIPPED;

        return new CreateChatRoomResponse.ActionButtons(
                false,
                canConfirmReceipt,
                null,
                showConfirmReceiptButton ? "CONFIRM_RECEIPT" : null,
                canShip,
                canConfirmReceipt,
                showShipButton ? "SHIP" : null,
                showConfirmReceiptButton ? "CONFIRM_RECEIPT" : null,
                payment.getStatus().name(),
                resolveActionNotice(payment, seller, buyer),
                shipDeadline,
                receiptDeadline
        );
    }

    private boolean isWinner(Long currentUserId, Optional<AuctionPayment> optionalPayment) {
        return optionalPayment
                .filter(this::isAuctionTradeReady)
                .map(payment -> payment.getBidderId().equals(currentUserId))
                .orElse(false);
    }

    private boolean isAuctionTradeReady(AuctionPayment payment) {
        return payment.getAuction().getStatus() == AuctionStatus.SUCCESSFUL_BID
                && payment.getBidderId().equals(payment.getAuction().getWinnerId());
    }

    private void syncAuctionPurchaseShipped(Long purchaseId) {
        if (purchaseId == null) {
            return;
        }
        Purchase purchase = purchaseRepository.findById(purchaseId).orElse(null);
        if (purchase == null || purchase.getStatus() != PurchaseStatus.PAID) {
            return;
        }
        try {
            purchase.markShipped();
        } catch (IllegalStateException ignored) {
        }
    }

    private void syncAuctionPurchaseCompleted(Long purchaseId) {
        if (purchaseId == null) {
            return;
        }
        Purchase purchase = purchaseRepository.findById(purchaseId).orElse(null);
        if (purchase == null || purchase.getStatus() == PurchaseStatus.CANCELED) {
            return;
        }
        try {
            if (purchase.getStatus() == PurchaseStatus.PAID) {
                purchase.markShipped();
            }
            purchase.markBuyerCompleted();
            purchase.complete();
            purchase.settle();
        } catch (IllegalStateException ignored) {
        }
    }

    private CreateChatRoomResponse.ActionButtons inactiveActionButtons(
            String status,
            OffsetDateTime shipDeadline,
            OffsetDateTime receiptDeadline
    ) {
        return new CreateChatRoomResponse.ActionButtons(
                false,
                false,
                null,
                null,
                false,
                false,
                null,
                null,
                status,
                null,
                shipDeadline,
                receiptDeadline
        );
    }

    private Optional<AuctionPayment> resolveAuctionPayment(
            ChatRoom room,
            ProductSummaryPort.ProductSummary product
    ) {
        if (product.auctionId() == null || resolveChatType(product) != ChatType.AUCTION) {
            return Optional.empty();
        }
        return resolveAuctionPayment(room, product, false);
    }

    private Optional<AuctionPayment> resolveAuctionPayment(
            ChatRoom room,
            ProductSummaryPort.ProductSummary product,
            boolean lock
    ) {
        if (product.auctionId() == null || resolveChatType(product) != ChatType.AUCTION) {
            return Optional.empty();
        }

        List<AuctionPaymentStatus> statuses = List.of(
                AuctionPaymentStatus.RESERVED,
                AuctionPaymentStatus.SHIPPED,
                AuctionPaymentStatus.SETTLED,
                AuctionPaymentStatus.REFUND_PENDING,
                AuctionPaymentStatus.REFUNDED,
                AuctionPaymentStatus.DISPUTED
        );

        if (lock) {
            return auctionPaymentRepository
                    .findFirstByAuctionAuctionIdAndBidderIdAndSellerIdAndStatusInAndDeletedAtIsNullOrderByReservedAtDescAuctionPaymentIdDesc(
                            product.auctionId(),
                            room.getBuyerId(),
                            room.getSellerId(),
                            statuses
                    );
        }

        return auctionPaymentRepository
                .findLatestByAuctionAndParticipantsAndStatuses(
                        product.auctionId(),
                        room.getBuyerId(),
                        room.getSellerId(),
                        statuses,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst();
    }

    private String resolveActionNotice(AuctionPayment payment, boolean seller, boolean buyer) {
        return switch (payment.getStatus()) {
            case RESERVED -> seller
                    ? "3일 안에 상품을 보내고 '보냈어요' 버튼을 눌러주세요. 기한 내 발송 처리가 되지 않으면 구매자에게 자동 환불됩니다."
                    : buyer
                    ? "판매자의 발송 처리를 기다리는 중입니다. 판매자가 3일 안에 발송 처리를 하지 않으면 자동으로 환불됩니다."
                    : null;
            case SHIPPED -> buyer
                    ? "판매자가 발송 처리를 완료했습니다. 7일 안에 상품 수령 후 '받았어요' 버튼을 눌러주세요. 기한 내 수령확정을 하지 않으면 자동으로 수령확정됩니다. 상품을 받지 못했다면 신고하기를 이용해주세요."
                    : seller
                    ? "발송 처리가 완료되었습니다. 구매자가 7일 안에 수령확정을 하지 않으면 자동으로 수령확정됩니다."
                    : null;
            case SETTLED -> "거래가 완료되었습니다.";
            case REFUND_PENDING -> "발송 처리 기한이 지나 환불 처리가 진행 중입니다.";
            case REFUNDED -> "구매자 환불이 완료되었습니다.";
            case DISPUTED -> "신고가 접수되어 거래 처리가 보류되었습니다.";
        };
    }

    private MemberSnapshot resolveMemberSnapshot(Long memberId) {
        return memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
                .map(member -> new MemberSnapshot(
                        normalizeNickname(member),
                        toPublicImageUrl(member.getProfileImage())
                ))
                .orElse(new MemberSnapshot("User#" + memberId, null));
    }

    private String toPublicImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrlService.toPublicUrl(imageUrl);
    }

    private String normalizeNickname(Member member) {
        String nickname = member.getNickname();
        if (nickname == null || nickname.isBlank()) {
            return "User#" + member.getMemberId();
        }
        return nickname;
    }

    private record MemberSnapshot(String nickname, String profileImageUrl) {
    }
}
