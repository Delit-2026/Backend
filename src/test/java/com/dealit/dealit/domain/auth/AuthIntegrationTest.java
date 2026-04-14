package com.dealit.dealit.domain.auth;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.global.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private Member savedMember;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();

		Member member = Member.create(
			"dealit-user",
			passwordEncoder.encode("Password123!"),
			"user@dealit.com",
			null,
			"홍길동"
		);
		savedMember = memberRepository.save(member);
		savedMember.assignDefaultNickname();
		memberRepository.save(savedMember);
	}

	@Test
	@DisplayName("로그인 성공 시 access token을 발급한다")
	void loginSuccess() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "dealit-user",
					  "password": "Password123!"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isString())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").isNumber());
	}

	@Test
	@DisplayName("로그인 실패 시 401을 반환한다")
	void loginFailsWhenPasswordMismatch() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "dealit-user",
					  "password": "WrongPassword123!"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	@DisplayName("JWT 없이 보호 API에 접근하면 401을 반환한다")
	void protectedApiFailsWithoutJwt() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	@DisplayName("유효한 JWT가 있으면 보호 API에 접근할 수 있다")
	void protectedApiSuccessWithJwt() throws Exception {
		String accessToken = jwtService.generateAccessToken(savedMember);

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").value(savedMember.getMemberId()))
			.andExpect(jsonPath("$.loginId").value("dealit-user"))
			.andExpect(jsonPath("$.email").value("user@dealit.com"))
			.andExpect(jsonPath("$.nickname").value(savedMember.getNickname()));
	}

	@Test
	@DisplayName("만료된 JWT는 401을 반환한다")
	void protectedApiFailsWhenJwtExpired() throws Exception {
		String expiredToken = jwtService.generateAccessToken(savedMember, Duration.ofMillis(-1));

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer " + expiredToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
	}

	@Test
	@DisplayName("위조된 JWT는 401을 반환한다")
	void protectedApiFailsWhenJwtInvalid() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer invalid.token.value"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}
}
