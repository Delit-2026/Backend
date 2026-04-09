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

		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorizationHeader.substring(7);

		try {
			JwtClaims claims = jwtService.parse(token);
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
		request.setAttribute("auth_error_code", code);
		request.setAttribute("auth_error_message", message);
		authenticationEntryPoint.commence(
			request,
			response,
			new InsufficientAuthenticationException(message)
		);
	}
}
