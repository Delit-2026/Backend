package com.dealit.dealit.domain.member;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberInterestCategoryRepository;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MemberSignUpIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberInterestCategoryRepository memberInterestCategoryRepository;

	@Autowired
	private FcmTokenRepository fcmTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		fcmTokenRepository.deleteAll();
		memberInterestCategoryRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("회원가입에 성공하면 암호화된 비밀번호와 기본 닉네임이 저장된다")
	void signUpSuccess() throws Exception {
		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "dealit-user",
					  "password": "Password123!",
					  "email": "user@dealit.com",
					  "name": "홍길동"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").isNumber())
			.andExpect(jsonPath("$.loginId").value("dealit-user"))
			.andExpect(jsonPath("$.email").value("user@dealit.com"))
			.andExpect(jsonPath("$.nickname").value(org.hamcrest.Matchers.startsWith("Dealit#")));

		Member savedMember = memberRepository.findAll().getFirst();
		assertThat(savedMember.getPassword()).isNotEqualTo("Password123!");
		assertThat(passwordEncoder.matches("Password123!", savedMember.getPassword())).isTrue();
		assertThat(savedMember.getNickname()).isEqualTo("Dealit#" + savedMember.getMemberId());
		assertThat(savedMember.isVerified()).isFalse();
	}

	@Test
	@DisplayName("관심 카테고리를 선택하면 회원가입 시 저장된다")
	void signUpSavesInterestCategories() throws Exception {
		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "interest-user",
					  "password": "Password123!",
					  "email": "interest@dealit.com",
					  "interestCategoryIds": [1, 2]
					}
					"""))
			.andExpect(status().isCreated());

		Member savedMember = memberRepository.findAll().getFirst();
		assertThat(memberInterestCategoryRepository.findAllByMemberIdOrderByCategoryIdAsc(savedMember.getMemberId()))
			.extracting("categoryId")
			.containsExactly(1L, 2L);
	}

	@Test
	@DisplayName("소분류나 중분류를 관심 카테고리로 보내면 실패한다")
	void signUpFailsWhenInterestCategoryIsNotTopLevel() throws Exception {
		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "invalid-interest-user",
					  "password": "Password123!",
					  "email": "invalid-interest@dealit.com",
					  "interestCategoryIds": [4]
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INTEREST_CATEGORY_REQUEST"));
	}

	@Test
	@DisplayName("회원가입 관심 카테고리 조회는 대분류만 반환한다")
	void getInterestCategoriesReturnsTopLevelOnly() throws Exception {
		mockMvc.perform(get("/api/v1/members/interest-categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[0].categoryId").value(1))
			.andExpect(jsonPath("$[1].categoryId").value(2))
			.andExpect(jsonPath("$[2].categoryId").value(3));
	}

	@Test
	@DisplayName("중복 이메일로 회원가입하면 충돌 오류를 반환한다")
	void signUpFailsWhenEmailDuplicated() throws Exception {
		memberRepository.save(Member.create(
			"existing-user",
			passwordEncoder.encode("Password123!"),
			"user@dealit.com",
			null,
			null
		));

		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "new-user",
					  "password": "Password123!",
					  "email": "user@dealit.com"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_MEMBER"))
			.andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
	}

	@Test
	@DisplayName("필수 입력값이 없으면 검증 오류를 반환한다")
	void signUpFailsWhenRequestInvalid() throws Exception {
		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "loginId": "",
					  "password": "1234",
					  "email": "invalid-email"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors").isArray())
			.andExpect(jsonPath("$.errors.length()").value(3));
	}

	@Test
	@DisplayName("사용 가능한 로그인 아이디이면 available true를 반환한다")
	void loginIdCheckReturnsAvailableTrue() throws Exception {
		mockMvc.perform(get("/api/v1/members/login-id/check")
				.param("loginId", "dealit-user"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.loginId").value("dealit-user"))
			.andExpect(jsonPath("$.available").value(true));
	}

	@Test
	@DisplayName("이미 사용 중인 로그인 아이디이면 available false를 반환한다")
	void loginIdCheckReturnsAvailableFalseWhenDuplicated() throws Exception {
		memberRepository.save(Member.create(
			"existing-user",
			passwordEncoder.encode("Password123!"),
			"user@dealit.com",
			null,
			null
		));

		mockMvc.perform(get("/api/v1/members/login-id/check")
				.param("loginId", "existing-user"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.loginId").value("existing-user"))
			.andExpect(jsonPath("$.available").value(false));
	}

	@Test
	@DisplayName("로그인 아이디 형식이 올바르지 않으면 검증 오류를 반환한다")
	void loginIdCheckFailsWhenLoginIdInvalid() throws Exception {
		mockMvc.perform(get("/api/v1/members/login-id/check")
				.param("loginId", ""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("사용 가능한 닉네임이면 available true를 반환한다")
	void nicknameCheckReturnsAvailableTrue() throws Exception {
		mockMvc.perform(get("/api/v1/members/nickname/check")
				.param("nickname", "비드마스터"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nickname").value("비드마스터"))
			.andExpect(jsonPath("$.available").value(true));
	}

	@Test
	@DisplayName("이미 사용 중인 닉네임이면 available false를 반환한다")
	void nicknameCheckReturnsAvailableFalseWhenDuplicated() throws Exception {
		Member member = memberRepository.save(Member.create(
			"existing-user",
			passwordEncoder.encode("Password123!"),
			"user@dealit.com",
			null,
			null
		));
		member.updateProfile(member.getName(), "비드마스터", null, null);
		memberRepository.save(member);

		mockMvc.perform(get("/api/v1/members/nickname/check")
				.param("nickname", "비드마스터"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nickname").value("비드마스터"))
			.andExpect(jsonPath("$.available").value(false));
	}

	@Test
	@DisplayName("닉네임 형식이 올바르지 않으면 검증 오류를 반환한다")
	void nicknameCheckFailsWhenNicknameInvalid() throws Exception {
		mockMvc.perform(get("/api/v1/members/nickname/check")
				.param("nickname", "a"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}
}
