package com.dealit.dealit.domain.chat.service;

import com.dealit.dealit.domain.chat.dto.ChatMessageListResponse;
import com.dealit.dealit.domain.chat.dto.ChatMessageResponse;
import com.dealit.dealit.domain.chat.dto.ChatRoomListItemResponse;
import com.dealit.dealit.domain.chat.dto.ChatRoomListResponse;
import com.dealit.dealit.domain.chat.dto.CreateChatRoomRequest;
import com.dealit.dealit.domain.chat.dto.CreateChatRoomResponse;
import com.dealit.dealit.domain.chat.dto.MarkChatRoomAsReadResponse;
import com.dealit.dealit.domain.chat.dto.ReportChatMessageRequest;
import com.dealit.dealit.domain.chat.dto.ReportChatMessageResponse;
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
import com.dealit.dealit.domain.chat.exception.DuplicateChatRoomException;
import com.dealit.dealit.domain.chat.exception.ProductNotFoundException;
import com.dealit.dealit.domain.chat.repository.ChatMessageReportRepository;
import com.dealit.dealit.domain.chat.repository.ChatMessageRepository;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageReportRepository chatMessageReportRepository;
    private final MemberRepository memberRepository;
    private final ProductOwnershipPort productOwnershipPort;
    private final ProductSummaryPort productSummaryPort;

    public CreateChatRoomResponse createChatRoom(CreateChatRoomRequest request, Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }
        if (request.productId() == null || request.receiverId() == null) {
            throw new IllegalArgumentException("productId, receiverId는 필수입니다.");
        }
        if (currentUserId.equals(request.receiverId())) {
            throw new IllegalArgumentException("자기 자신과는 채팅방을 생성할 수 없습니다.");
        }

        Long sellerId = resolveSellerIdFromProduct(request.productId(), currentUserId, request.receiverId());
        Long buyerId = sellerId.equals(currentUserId) ? request.receiverId() : currentUserId;

        if (sellerId.equals(buyerId)) {
            throw new IllegalArgumentException("seller/buyer가 동일할 수 없습니다.");
        }

        boolean duplicated = chatRoomRepository
                .findBySellerIdAndBuyerIdAndProductIdAndDeletedAtIsNull(
                        sellerId, buyerId, request.productId()
                ).isPresent();

        if (duplicated) {
            throw new DuplicateChatRoomException("이미 동일 상품/참여자의 채팅방이 존재합니다.");
        }

        ChatRoom saved = chatRoomRepository.save(
                ChatRoom.create(sellerId, buyerId, request.productId(), ChatType.GENERAL)
        );

        MemberSnapshot currentUser = resolveMemberSnapshot(currentUserId);
        MemberSnapshot receiver = resolveMemberSnapshot(request.receiverId());
        ProductSummaryPort.ProductSummary product = resolveProductSummaryOrFallback(request.productId());

        return new CreateChatRoomResponse(
                saved.getRoomId(),
                saved.getChatType(),
                new CreateChatRoomResponse.ProductInfo(
                        product.productId(),
                        product.name(),
                        product.thumbnailUrl(),
                        "GENERAL",
                        "ACTIVE"
                ),
                List.of(
                        new CreateChatRoomResponse.ParticipantInfo(
                                currentUserId,
                                currentUser.nickname(),
                                sellerId.equals(currentUserId) ? "SELLER" : "BUYER"
                        ),
                        new CreateChatRoomResponse.ParticipantInfo(
                                request.receiverId(),
                                receiver.nickname(),
                                sellerId.equals(request.receiverId()) ? "SELLER" : "BUYER"
                        )
                ),
                false,
                new CreateChatRoomResponse.ActionButtons(
                        false,
                        false,
                        null,
                        null
                ),
                LocalDateTime.now()
        );
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
                .map(room -> {
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
                                    product.thumbnailUrl()
                            ),
                            room.getChatType(),
                            lastMessageInfo,
                            unreadCount,
                            room.getUpdatedAt()
                    );
                })
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
                .map(m -> new ChatMessageResponse(
                        m.getMessageId(),
                        m.getSenderId(),
                        m.getSenderNickname(),
                        m.getMessageType(),
                        m.getContent(),
                        m.isRead(),
                        m.getSentAt()
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

        return new SendChatMessageResponse(
                saved.getMessageId(),
                saved.getRoomId(),
                saved.getSenderId(),
                saved.getMessageType(),
                saved.getContent(),
                saved.isRead(),
                saved.getSentAt()
        );
    }

    public MarkChatRoomAsReadResponse markAsRead(Long roomId, Long currentUserId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));

        chatMessageRepository.markAllAsRead(roomId, currentUserId);

        long unreadAfter = chatMessageRepository.countUnreadByRoomId(roomId, currentUserId);
        int unreadCountAfter = unreadAfter > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) unreadAfter;

        return new MarkChatRoomAsReadResponse(
                roomId,
                unreadCountAfter,
                LocalDateTime.now()
        );
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
                LocalDateTime.now()
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
                LocalDateTime.now()
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

        message.softDelete();
    }

    private Long resolveSellerIdFromProduct(Long productId, Long currentUserId, Long receiverId) {
        Long ownerId = productOwnershipPort.getOwnerIdByProductId(productId);

        if (!ownerId.equals(currentUserId) && !ownerId.equals(receiverId)) {
            throw new IllegalArgumentException("상품 소유자는 채팅 참여자 중 한 명이어야 합니다.");
        }
        return ownerId;
    }

    private ProductSummaryPort.ProductSummary resolveProductSummaryOrFallback(Long productId) {
        try {
            return productSummaryPort.getSummaryByProductId(productId);
        } catch (ProductNotFoundException e) {
            return new ProductSummaryPort.ProductSummary(productId, null, null);
        }
    }

    private MemberSnapshot resolveMemberSnapshot(Long memberId) {
        return memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
                .map(member -> new MemberSnapshot(
                        normalizeNickname(member),
                        member.getProfileImage()
                ))
                .orElse(new MemberSnapshot("User#" + memberId, null));
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