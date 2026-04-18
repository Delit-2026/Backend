package com.dealit.dealit.global.security.websocket;

import com.dealit.dealit.domain.member.repository.MemberRepository;
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
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final MemberRepository memberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
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

        return message;
    }

    private String resolveAuthorizationHeader(StompHeaderAccessor accessor) {
        List<String> values = accessor.getNativeHeader("Authorization");
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}