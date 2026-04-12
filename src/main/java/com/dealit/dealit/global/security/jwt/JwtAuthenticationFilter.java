package com.dealit.dealit.global.security.jwt;

import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final MemberRepository memberRepository;
	private final JwtAuthenticationEntryPoint authenticationEntryPoint;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String authorizationHeader = request.getHeader("Authorization");

		// Bearer 토큰이 없으면 인증 시도 없이 다음 필터로 넘긴다.
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorizationHeader.substring(7);

		try {
			// 서명/만료 검증 후 클레임을 추출한다.
			JwtClaims claims = jwtService.parse(token);
			// 탈퇴(soft delete)된 사용자는 토큰이 있어도 인증하지 않는다.
			if (memberRepository.findByMemberIdAndDeletedAtIsNull(claims.memberId()).isEmpty()) {
				commenceUnauthorized(request, response, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
				return;
			}

			AuthenticatedMember principal = new AuthenticatedMember(
				claims.memberId(),
				claims.loginId(),
				claims.role()
			);

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				List.of(new SimpleGrantedAuthority(claims.role()))
			);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			// 이후 인가 과정(@PreAuthorize, URL 권한 검사)에서 사용할 인증 정보를 저장한다.
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (ExpiredJwtException exception) {
			commenceUnauthorized(request, response, "TOKEN_EXPIRED", "만료된 토큰입니다.");
			return;
		} catch (JwtException | IllegalArgumentException exception) {
			commenceUnauthorized(request, response, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void commenceUnauthorized(
		HttpServletRequest request,
		HttpServletResponse response,
		String code,
		String message
	) throws IOException {
		// EntryPoint에서 일관된 에러 응답을 만들 수 있도록 request attribute에 원인을 전달한다.
		request.setAttribute("auth_error_code", code);
		request.setAttribute("auth_error_message", message);
		authenticationEntryPoint.commence(
			request,
			response,
			new InsufficientAuthenticationException(message)
		);
	}
}
