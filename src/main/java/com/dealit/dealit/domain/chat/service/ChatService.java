package com.dealit.dealit.domain.chat.service;

import com.dealit.dealit.domain.chat.dto.*;
import com.dealit.dealit.domain.chat.repository.ChatMessageReportRepository;
import com.dealit.dealit.domain.chat.repository.ChatMessageRepository;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.domain.chat.entity.ChatRoom;
import com.dealit.dealit.domain.chat.entity.ChatType;
import com.dealit.dealit.domain.chat.exception.DuplicateChatRoomException;
import com.dealit.dealit.domain.chat.dto.ChatRoomListItemResponse;
import com.dealit.dealit.domain.chat.dto.ChatRoomListResponse;
import com.dealit.dealit.domain.chat.entity.ChatMessage;
import com.dealit.dealit.domain.chat.dto.ChatMessageListResponse;
import com.dealit.dealit.domain.chat.dto.ChatMessageResponse;
import com.dealit.dealit.domain.chat.exception.ChatRoomNotFoundException;
import com.dealit.dealit.domain.chat.dto.SendChatMessageRequest;
import com.dealit.dealit.domain.chat.dto.SendChatMessageResponse;
import com.dealit.dealit.domain.chat.dto.ReportChatMessageRequest;
import com.dealit.dealit.domain.chat.dto.ReportChatMessageResponse;
import com.dealit.dealit.domain.chat.entity.ChatMessageReport;
import com.dealit.dealit.domain.chat.exception.ChatForbiddenException;
import com.dealit.dealit.domain.chat.exception.ChatMessageNotFoundException;
import com.dealit.dealit.domain.chat.exception.DuplicateChatReportException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageReportRepository chatMessageReportRepository;

    /**
     * 채팅방 생성
     */
    public CreateChatRoomResponse createChatRoom(CreateChatRoomRequest request, Long currentUserId) {
        // 1) 기본 검증
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }
        if (request.productId() == null || request.receiverId() == null) {
            throw new IllegalArgumentException("productId, receiverId는 필수입니다.");
        }
        if (currentUserId.equals(request.receiverId())) {
            throw new IllegalArgumentException("자기 자신과는 채팅방을 생성할 수 없습니다.");
        }

        // 2) 중복 채팅방 체크 (양방향)
        Long sellerId = currentUserId;      // TODO: 실제로는 상품 소유자 기반으로 결정
        Long buyerId = request.receiverId(); // TODO: 실제로는 currentUser/receiver 역할 판별 필요

        boolean duplicated =
                chatRoomRepository.findBySellerIdAndBuyerIdAndProductIdAndDeletedAtIsNull(
                        sellerId, buyerId, request.productId()
                ).isPresent()
                        || chatRoomRepository.findBySellerIdAndBuyerIdAndProductIdAndDeletedAtIsNull(
                        buyerId, sellerId, request.productId()
                ).isPresent();

        if (duplicated) {
            throw new DuplicateChatRoomException("이미 동일 상품/참여자의 채팅방이 존재합니다.");
        }

        // 3) 저장
        ChatRoom saved = chatRoomRepository.save(
                ChatRoom.create(sellerId, buyerId, request.productId(), ChatType.GENERAL)
        );

        // 4) 응답 (최소 구현; product/participant 상세는 추후 User/Product 조회 붙여서 고도화)
        return new CreateChatRoomResponse(
                saved.getRoomId(),
                saved.getChatType(),
                new CreateChatRoomResponse.ProductInfo(
                        request.productId(),
                        "",
                        null,
                        "GENERAL",
                        "ACTIVE"
                ),
                List.of(
                        new CreateChatRoomResponse.ParticipantInfo(currentUserId, "나", "SELLER"),
                        new CreateChatRoomResponse.ParticipantInfo(request.receiverId(), "상대", "BUYER")
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

    /**
     * 채팅방 목록 조회 (페이징)
     */
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

                    ChatMessage lastMessage = chatMessageRepository.findLatestMessage(room.getRoomId()).orElse(null);

                    int unreadCount = (int) chatMessageRepository.countUnreadByRoomId(room.getRoomId(), currentUserId);

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
                                    "상대",     // TODO: member 조회 붙이면 실제 닉네임
                                    null        // TODO: member 조회 붙이면 프로필 이미지
                            ),
                            new ChatRoomListItemResponse.ProductInfo(
                                    room.getProductId(),
                                    "",         // TODO: product 조회 붙이면 상품명
                                    null        // TODO: product 조회 붙이면 썸네일
                            ),
                            room.getChatType(),
                            lastMessageInfo,
                            unreadCount,
                            room.getUpdatedAt() // BaseEntity updatedAt 사용
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

    /**
     * 메시지 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public ChatMessageListResponse getChatMessages(Long roomId, Long currentUserId, int page, int size) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        // room 존재 + 참여 권한 체크
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

    /**
     * 메시지 전송
     */
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

        // room 존재 + 참여 권한 체크
        ChatRoom room = chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));

        // 메시지 생성/저장
        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.create(
                        room.getRoomId(),
                        currentUserId,
                        "나", // TODO: member 조회 연동 후 실제 닉네임으로 교체
                        request.messageType(),
                        request.content()
                )
        );

        // (선택) room updatedAt 갱신이 필요하면 room 엔티티에 touch() 메서드 추가 후 호출
        // room.touch();

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

    /**
     * 읽음 처리
     */
    public MarkChatRoomAsReadResponse markAsRead(Long roomId, Long currentUserId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        // room 존재 + 참여 권한 확인
        chatRoomRepository.findAccessibleRoom(roomId, currentUserId)
                .orElseThrow(() -> new ChatRoomNotFoundException("접근 가능한 채팅방이 없습니다."));

        // 내가 보낸 메시지는 제외하고 읽음 처리
        chatMessageRepository.markAllAsRead(roomId, currentUserId);

        return new MarkChatRoomAsReadResponse(
                roomId,
                0,
                LocalDateTime.now()
        );
    }

    /**
     * 전체 안읽음 개수
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        long count = chatMessageRepository.countTotalUnreadForUser(currentUserId);

        // DTO가 int라서 안전 변환
        int totalUnread = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;

        return new UnreadCountResponse(
                totalUnread,
                LocalDateTime.now()
        );
    }

    /**
     * 메시지 신고
     */
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

        // 메시지 존재 확인
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ChatMessageNotFoundException("신고할 메시지를 찾을 수 없습니다."));

        // 중복 신고 방지
        boolean alreadyReported = chatMessageReportRepository.existsActiveReport(messageId, currentUserId);
        if (alreadyReported) {
            throw new DuplicateChatReportException("이미 신고한 메시지입니다.");
        }

        // 신고 저장
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

    /**
     * 메시지 삭제 (선택 API)
     */
    public void deleteMessage(Long messageId, Long currentUserId) {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId는 필수입니다.");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("인증 사용자 정보가 없습니다.");
        }

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ChatMessageNotFoundException("삭제할 메시지를 찾을 수 없습니다."));

        if (!message.getSenderId().equals(currentUserId)) {
            throw new ChatForbiddenException("본인이 보낸 메시지만 삭제할 수 있습니다.");
        }

        // 현재는 하드 삭제
        // TODO: 프로젝트 soft-delete 규칙에 맞춰 전환 필요
        chatMessageRepository.delete(message);
    }
}