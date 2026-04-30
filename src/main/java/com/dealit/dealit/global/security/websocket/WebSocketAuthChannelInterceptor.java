package com.dealit.dealit.global.security.websocket;

import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.global.security.AuthenticatedMember;
import com.dealit.dealit.global.security.jwt.JwtClaims;
import com.dealit.dealit.global.security.jwt.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String CHAT_ROOM_TOPIC_PREFIX = "/topic/chats/rooms/";

    private final JwtService jwtService;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authorization = resolveAuthorizationHeader(accessor);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new InsufficientAuthenticationException("WebSocket 연결을 위한 인증이 필요합니다.");
        }

        String token = authorization.substring(7);
        try {
            JwtClaims claims = jwtService.parse(token);
            if (memberRepository.findByMemberIdAndDeletedAtIsNull(claims.memberId()).isEmpty()) {
                throw new InsufficientAuthenticationException("유효하지 않은 토큰입니다.");
            }

            AuthenticatedMember principal = new AuthenticatedMember(
                    claims.memberId(),
                    claims.loginId(),
                    claims.role()
            );

            Principal authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority(claims.role()))
            );
            accessor.setUser(authentication);
        } catch (ExpiredJwtException e) {
            throw new InsufficientAuthenticationException("만료된 토큰입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InsufficientAuthenticationException("유효하지 않은 토큰입니다.");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(CHAT_ROOM_TOPIC_PREFIX)) {
            return;
        }

        Long roomId = resolveRoomId(destination);
        Long currentUserId = resolveCurrentUserId(accessor.getUser());

        boolean accessible = chatRoomRepository.findAccessibleRoom(roomId, currentUserId).isPresent();
        if (!accessible) {
            throw new AccessDeniedException("접근 권한이 없는 채팅방입니다.");
        }
    }

    private Long resolveRoomId(String destination) {
        String roomIdValue = destination.substring(CHAT_ROOM_TOPIC_PREFIX.length());
        if (roomIdValue.isBlank() || roomIdValue.contains("/")) {
            throw new AccessDeniedException("유효하지 않은 채팅방 구독 경로입니다.");
        }

        try {
            return Long.valueOf(roomIdValue);
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("유효하지 않은 채팅방 구독 경로입니다.");
        }
    }

    private Long resolveCurrentUserId(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new InsufficientAuthenticationException("WebSocket 구독을 위한 인증이 필요합니다.");
        }
        Object p = authentication.getPrincipal();
        if (!(p instanceof AuthenticatedMember member)) {
            throw new InsufficientAuthenticationException("WebSocket 구독을 위한 인증이 필요합니다.");
        }
        return member.memberId();
    }

    private String resolveAuthorizationHeader(StompHeaderAccessor accessor) {
        List<String> values = accessor.getNativeHeader("Authorization");
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
