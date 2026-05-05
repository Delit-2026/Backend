package com.dealit.dealit.domain.member;

import com.dealit.dealit.domain.member.LocationSource;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.entity.MemberInterestCategory;
import com.dealit.dealit.domain.member.repository.MemberInterestCategoryRepository;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.repository.FcmTokenRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileIntegrationTest {

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

	@Autowired
	private JwtService jwtService;

	@Value("${app.images.storage-root}")
	private String imageStorageRoot;

	private Member savedMember;
	private String accessToken;

	@BeforeEach
	void setUp() throws IOException {
		fcmTokenRepository.deleteAll();
		memberInterestCategoryRepository.deleteAll();
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
			savedMember.getName(),
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
	@DisplayName("JWT로 마이페이지 프로필을 조회하면 현재 프로필 응답을 반환한다")
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
	@DisplayName("프로필 수정이 성공하면 변경된 프로필을 반환하고 저장한다")
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
	@DisplayName("프로필 수정 요청에서 소개글과 이미지가 빠지면 기존 값을 유지한다")
	void updateProfilePreservesBioAndImageWhenFieldsMissing() throws Exception {
		savedMember.updateProfile(
			savedMember.getName(),
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
	@DisplayName("다른 회원이 쓰는 닉네임으로 프로필을 수정하면 409를 반환한다")
	void updateProfileFailsWhenNicknameDuplicated() throws Exception {
		Member otherMember = memberRepository.save(Member.create(
			"other-user",
			passwordEncoder.encode("Password123!"),
			"other@dealit.com",
			null,
			null
		));
		otherMember.updateProfile(otherMember.getName(), "이미있는닉네임", null, null);
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
	@DisplayName("현재 본인 닉네임으로 다시 저장하는 것은 허용한다")
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
	@DisplayName("구조화된 지역 수정이 성공하면 대표 location과 상세 필드를 함께 저장한다")
	void updateLocationSuccess() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me/location")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "postalCode": "21984",
					  "roadAddress": "인천광역시 연수구 센트럴로 123",
					  "jibunAddress": "인천광역시 연수구 송도동 24-1",
					  "detailAddress": "101동 1203호",
					  "locationSource": "POSTCODE"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.location").value("인천광역시 연수구 센트럴로 123 101동 1203호"))
			.andExpect(jsonPath("$.postalCode").value("21984"))
			.andExpect(jsonPath("$.roadAddress").value("인천광역시 연수구 센트럴로 123"))
			.andExpect(jsonPath("$.jibunAddress").value("인천광역시 연수구 송도동 24-1"))
			.andExpect(jsonPath("$.detailAddress").value("101동 1203호"))
			.andExpect(jsonPath("$.locationSource").value("POSTCODE"));

		Member updatedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
		assertThat(updatedMember.getLocation()).isEqualTo("인천광역시 연수구 센트럴로 123 101동 1203호");
		assertThat(updatedMember.getPostalCode()).isEqualTo("21984");
		assertThat(updatedMember.getRoadAddress()).isEqualTo("인천광역시 연수구 센트럴로 123");
		assertThat(updatedMember.getJibunAddress()).isEqualTo("인천광역시 연수구 송도동 24-1");
		assertThat(updatedMember.getDetailAddress()).isEqualTo("101동 1203호");
		assertThat(updatedMember.getLocationSource()).isEqualTo(LocationSource.POSTCODE);
	}

	@Test
	@DisplayName("기존 location 문자열만 보내도 지역 수정이 동작한다")
	void updateLocationLegacyStringOnlySuccess() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me/location")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "location": "서울특별시 마포구"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.location").value("서울특별시 마포구"))
			.andExpect(jsonPath("$.postalCode").doesNotExist())
			.andExpect(jsonPath("$.roadAddress").doesNotExist())
			.andExpect(jsonPath("$.locationSource").value("MANUAL"));

		Member updatedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
		assertThat(updatedMember.getLocation()).isEqualTo("서울특별시 마포구");
		assertThat(updatedMember.getRoadAddress()).isNull();
		assertThat(updatedMember.getLocationSource()).isEqualTo(LocationSource.MANUAL);
	}

	@Test
	@DisplayName("내 지역 조회는 구조화된 위치 정보를 함께 반환한다")
	void getMyLocationSuccess() throws Exception {
		savedMember.updateLocationDetails(
			"경기도 성남시 분당구 판교역로 166 101동 1203호",
			"13529",
			"경기도 성남시 분당구 판교역로 166",
			"경기도 성남시 분당구 백현동 532",
			"101동 1203호",
			LocationSource.POSTCODE
		);
		memberRepository.save(savedMember);

		mockMvc.perform(get("/api/v1/users/me/location")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.location").value("경기도 성남시 분당구 판교역로 166 101동 1203호"))
			.andExpect(jsonPath("$.postalCode").value("13529"))
			.andExpect(jsonPath("$.roadAddress").value("경기도 성남시 분당구 판교역로 166"))
			.andExpect(jsonPath("$.jibunAddress").value("경기도 성남시 분당구 백현동 532"))
			.andExpect(jsonPath("$.detailAddress").value("101동 1203호"))
			.andExpect(jsonPath("$.locationSource").value("POSTCODE"));
	}

	@Test
	@DisplayName("내 관심 카테고리 조회 시 저장된 대분류 목록을 반환한다")
	void getMyInterestCategoriesSuccess() throws Exception {
		memberInterestCategoryRepository.saveAll(List.of(
			MemberInterestCategory.create(savedMember.getMemberId(), 1L),
			MemberInterestCategory.create(savedMember.getMemberId(), 2L)
		));

		mockMvc.perform(get("/api/v1/users/me/interest-categories")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].categoryId").value(1))
			.andExpect(jsonPath("$[1].categoryId").value(2));
	}

	@Test
	@DisplayName("프로필 이미지 업로드에 성공하면 접근 가능한 URL을 반환하고 회원 프로필에 저장한다")
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
			.andExpect(jsonPath("$.profileImageUrl").value(startsWith("http://localhost:8080/uploads/profile/images/")))
			.andExpect(jsonPath("$.profileImageUrl").value(org.hamcrest.Matchers.endsWith(".png")));

		Member updatedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
		assertThat(updatedMember.getProfileImage()).startsWith("/uploads/profile/images/");

		mockMvc.perform(get(updatedMember.getProfileImage()))
			.andExpect(status().isOk())
			.andExpect(content().bytes(imageBytes));
	}

	@Test
	@DisplayName("프로필 이미지 원본 파일명이 비어 있어도 기본 파일명으로 업로드한다")
	void uploadProfileImageSuccessWhenOriginalFilenameBlank() throws Exception {
		byte[] imageBytes = "profile-image".getBytes();
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"",
			MediaType.IMAGE_PNG_VALUE,
			imageBytes
		);

		mockMvc.perform(multipart("/api/v1/users/me/profile-image")
				.file(file)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profileImageUrl").value(startsWith("http://localhost:8080/uploads/profile/images/")));

		Member updatedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
		assertThat(updatedMember.getProfileImage()).endsWith(".jpg");
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
