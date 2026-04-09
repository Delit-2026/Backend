package com.dealit.dealit.global.security.jwt;

import com.dealit.dealit.global.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		// 필터에서 넣어둔 에러 코드를 우선 사용하고, 없으면 기본 인증 오류 코드로 응답한다.
		String code = (String) request.getAttribute("auth_error_code");
		String message = (String) request.getAttribute("auth_error_message");

		ErrorResponse body = ErrorResponse.of(
			HttpStatus.UNAUTHORIZED.value(),
			code != null ? code : "AUTHENTICATION_REQUIRED",
			message != null ? message : "인증이 필요합니다.",
			List.of()
		);

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		// Security 예외를 프론트에서 처리하기 쉬운 JSON 형식으로 통일한다.
		objectMapper.writeValue(response.getWriter(), body);
	}
}
