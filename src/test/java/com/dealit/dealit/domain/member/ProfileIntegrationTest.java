package com.dealit.dealit.domain.member;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.global.security.jwt.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ProfileIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Value("${app.images.storage-root}")
	private String imageStorageRoot;

	private Member savedMember;
	private String accessToken;

	@BeforeEach
	void setUp() throws IOException {
		memberRepository.deleteAll();
		deleteStoredImages();

		Member member = Member.create(
			"dealit-user",
			passwordEncoder.encode("Password123!"),
			"user@dealit.com",
			null,
			"홍길동"
		);
		savedMember = memberRepository.save(member);
		savedMember.assignDefaultNickname();
		savedMember.updateProfile(
			savedMember.getNickname(),
			"좋은 거래 부탁드려요.",
			null
		);
		savedMember.updateLocation("서울특별시 강남구");
		savedMember = memberRepository.save(savedMember);
		accessToken = jwtService.generateAccessToken(savedMember);
	}

	@AfterEach
	void tearDown() throws IOException {
		deleteStoredImages();
	}

	@Test
	@DisplayName("JWT로 마이페이지 프로필을 조회하면 프론트가 기대하는 프로필 응답을 반환한다")
	void getMyPageProfileSuccess() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/mypage")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(savedMember.getMemberId()))
			.andExpect(jsonPath("$.nickname").value(savedMember.getNickname()))
			.andExpect(jsonPath("$.email").value("user@dealit.com"))
			.andExpect(jsonPath("$.bio").value("좋은 거래 부탁드려요."))
			.andExpect(jsonPath("$.profileImageUrl").doesNotExist())
			.andExpect(jsonPath("$.location").value("서울특별시 강남구"))
			.andExpect(jsonPath("$.rating").value(0.0))
			.andExpect(jsonPath("$.warningCount").value(0))
			.andExpect(jsonPath("$.biddingCount").value(0))
			.andExpect(jsonPath("$.sellingCount").value(0))
			.andExpect(jsonPath("$.wishlistCount").value(0));
	}

	@Test
	@DisplayName("JWT 없이 마이페이지 프로필을 조회하면 401을 반환한다")
	void getMyPageProfileFailsWithoutJwt() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/mypage"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	@DisplayName("프로필 수정에 성공하면 변경된 프로필을 반환하고 저장한다")
	void updateProfileSuccess() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nickname": "비드마스터",
					  "bio": "빠르고 매너 있는 거래를 좋아합니다.",
					  "profileImageUrl": "http://localhost:8080/profile/images/sample.png"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nickname").value("비드마스터"))
			.andExpect(jsonPath("$.bio").value("빠르고 매너 있는 거래를 좋아합니다."))
			.andExpect(jsonPath("$.profileImageUrl").value("http://localhost:8080/profile/images/sample.png"))
			.andExpect(jsonPath("$.location").value("서울특별시 강남구"));

		Member updatedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
		assertThat(updatedMember.getNickname()).isEqualTo("비드마스터");
		assertThat(updatedMember.getIntro()).isEqualTo("빠르고 매너 있는 거래를 좋아합니다.");
		assertThat(updatedMember.getProfileImage()).isEqualTo("/profile/images/sample.png");
		assertThat(updatedMember.getLocation()).isEqualTo("서울특별시 강남구");
	}

	@Test
	@DisplayName("프로필 수정 요청에서 소개글과 이미지가 누락되면 기존 값을 유지한다")
	void updateProfilePreservesBioAndImageWhenFieldsMissing() throws Exception {
		savedMember.updateProfile(
			savedMember.getNickname(),
			"기존 소개글",
			"/profile/images/existing.png"
		);
		memberRepository.save(savedMember);

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nickname": "새닉네임"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nickname").value("새닉네임"))
			.andExpect(jsonPath("$.bio").value("기존 소개글"))
			.andExpect(jsonPath("$.profileImageUrl").value("http://localhost:8080/profile/images/existing.png"));

		Member updatedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
		assertThat(updatedMember.getIntro()).isEqualTo("기존 소개글");
		assertThat(updatedMember.getProfileImage()).isEqualTo("/profile/images/existing.png");
	}

	@Test
	@DisplayName("다른 회원이 사용 중인 닉네임으로 프로필을 수정하면 409를 반환한다")
	void updateProfileFailsWhenNicknameDuplicated() throws Exception {
		Member otherMember = memberRepository.save(Member.create(
			"other-user",
			passwordEncoder.encode("Password123!"),
			"other@dealit.com",
			null,
			null
		));
		otherMember.updateProfile("이미있는닉네임", null, null);
		memberRepository.save(otherMember);

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nickname": "이미있는닉네임",
					  "bio": "중복 닉네임 테스트",
					  "profileImageUrl": null
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"));
	}

	@Test
	@DisplayName("현재 본인의 닉네임으로 프로필을 다시 저장하는 것은 허용한다")
	void updateProfileAllowsCurrentNickname() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nickname": "%s",
					  "bio": "기존 닉네임 유지",
					  "profileImageUrl": null
					}
					""".formatted(savedMember.getNickname())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nickname").value(savedMember.getNickname()))
			.andExpect(jsonPath("$.bio").value("기존 닉네임 유지"));
	}

	@Test
	@DisplayName("지역 수정에 성공하면 변경된 지역을 반환하고 저장한다")
	void updateLocationSuccess() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me/location")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "location": "인천광역시 연수구"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.location").value("인천광역시 연수구"));

		Member updatedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
		assertThat(updatedMember.getLocation()).isEqualTo("인천광역시 연수구");
	}

	@Test
	@DisplayName("프로필 이미지 업로드에 성공하면 접근 가능한 public URL을 반환하고 회원 프로필에 저장한다")
	void uploadProfileImageSuccess() throws Exception {
		byte[] imageBytes = "profile-image".getBytes();
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.png",
			MediaType.IMAGE_PNG_VALUE,
			imageBytes
		);

		mockMvc.perform(multipart("/api/v1/users/me/profile-image")
				.file(file)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profileImageUrl").value(startsWith("http://localhost:8080/profile/images/")));

		Member updatedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
		assertThat(updatedMember.getProfileImage()).startsWith("/profile/images/");

		mockMvc.perform(get(updatedMember.getProfileImage()))
			.andExpect(status().isOk())
			.andExpect(content().bytes(imageBytes));
	}

	@Test
	@DisplayName("허용되지 않은 프로필 이미지 형식이면 400을 반환한다")
	void uploadProfileImageFailsWhenContentTypeInvalid() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.gif",
			"image/gif",
			"gif-image".getBytes()
		);

		mockMvc.perform(multipart("/api/v1/users/me/profile-image")
				.file(file)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_PROFILE_REQUEST"));
	}

	private void deleteStoredImages() throws IOException {
		Path storageRoot = Path.of(imageStorageRoot);
		if (!Files.exists(storageRoot)) {
			return;
		}

		try (var paths = Files.walk(storageRoot)) {
			paths.sorted(Comparator.reverseOrder())
				.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException exception) {
						throw new RuntimeException(exception);
					}
				});
		}
	}
}
