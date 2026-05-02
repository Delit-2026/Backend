package com.dealit.dealit.domain.auction;

import com.dealit.dealit.domain.auction.repository.AuctionRepository;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.repository.ProductImageRepository;
import com.dealit.dealit.domain.product.repository.ProductRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuctionIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuctionRepository auctionRepository;

	@Autowired
	private ProductImageRepository productImageRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Value("${app.images.storage-root}")
	private String imageStorageRoot;

	private ProductImage uploadedImage;
	private String accessToken;

	@BeforeEach
	void setUp() throws IOException {
		auctionRepository.deleteAll();
		productImageRepository.deleteAll();
		productRepository.deleteAll();
		memberRepository.deleteAll();
		deleteStoredImages();

		Member member = Member.create(
			"auction-user",
			passwordEncoder.encode("Password123!"),
			"auction-user@dealit.com",
			null,
			"경매판매자"
		);
		Member savedMember = memberRepository.save(member);
		savedMember.assignDefaultNickname();
		savedMember.verifyEmail();
		savedMember = memberRepository.save(savedMember);
		accessToken = jwtService.generateAccessToken(savedMember);

		uploadedImage = productImageRepository.save(
			ProductImage.createTemporary(
				"/uploads/auction/images/test-image.jpg",
				"test-image.jpg",
				savedMember.getMemberId()
			)
		);
	}

	@AfterEach
	void tearDown() throws IOException {
		deleteStoredImages();
	}

	@Test
	@DisplayName("내 판매 중 경매 목록 조회는 현재 사용자가 등록한 진행 중 경매를 반환한다")
	void getMySellingAuctionProductsReturnsCurrentMemberLiveAuctions() throws Exception {
		mockMvc.perform(post("/api/v1/auction")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "Nintendo Switch OLED",
					  "description": "Includes dock and controller.",
					  "saleType": "AUCTION",
					  "categoryId": 21,
					  "price": null,
					  "startPrice": 180000,
					  "auctionDurationDays": 2,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "http://localhost:8080/uploads/auction/images/test-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "서울 마포구",
					  "draftId": null
					}
					""".formatted(uploadedImage.getImageId())))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/mypage/auctions/selling")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].productId").isNumber())
			.andExpect(jsonPath("$.content[0].auctionId").isNumber())
			.andExpect(jsonPath("$.content[0].name").value("Nintendo Switch OLED"))
			.andExpect(jsonPath("$.content[0].auctionStatus").value("AUCTION_LIVE"));
	}

	@Test
	@DisplayName("이메일 미인증 회원은 경매 상품을 등록할 수 없다")
	void createAuctionFailsWhenEmailNotVerified() throws Exception {
		Member member = Member.create(
			"auction-unverified-user",
			passwordEncoder.encode("Password123!"),
			"auction-unverified@dealit.com",
			null,
			"미인증경매회원"
		);
		Member savedMember = memberRepository.save(member);
		savedMember.assignDefaultNickname();
		savedMember = memberRepository.save(savedMember);
		String unverifiedAccessToken = jwtService.generateAccessToken(savedMember);

		ProductImage unverifiedImage = productImageRepository.save(
			ProductImage.createTemporary(
				"/uploads/auction/images/unverified-image.jpg",
				"unverified-image.jpg",
				savedMember.getMemberId()
			)
		);

		mockMvc.perform(post("/api/v1/auction")
				.header("Authorization", "Bearer " + unverifiedAccessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "미인증 경매 상품",
					  "description": "이메일 인증 전 등록 시도",
					  "saleType": "AUCTION",
					  "categoryId": 21,
					  "price": null,
					  "startPrice": 180000,
					  "auctionDurationDays": 2,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "http://localhost:8080/uploads/auction/images/unverified-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "서울 마포구",
					  "draftId": null
					}
					""".formatted(unverifiedImage.getImageId())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
	}

	@Test
	@DisplayName("이미지 업로드 응답은 접근 가능한 공개 URL을 반환한다")
	void uploadAuctionImageReturnsAccessibleUrl() throws Exception {
		byte[] imageBytes = "test-image".getBytes();
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"auction-upload.png",
			MediaType.IMAGE_PNG_VALUE,
			imageBytes
		);

		mockMvc.perform(multipart("/api/v1/auction/image")
				.file(file)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageId").isNumber())
			.andExpect(jsonPath("$.imageUrl").value(startsWith("http://localhost:8080/uploads/auction/images/")))
			.andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.endsWith(".png")));

		ProductImage savedImage = productImageRepository.findAll().stream()
			.filter(image -> image.getOriginalFileName().equals("auction-upload.png"))
			.findFirst()
			.orElseThrow();

		mockMvc.perform(get(savedImage.getImageUrl()))
			.andExpect(status().isOk())
			.andExpect(content().bytes(imageBytes));
	}

	@Test
	@DisplayName("임시 업로드한 경매 이미지는 imageId 기준으로 삭제된다")
	void deleteAuctionImageSuccess() throws Exception {
		byte[] imageBytes = "delete-me".getBytes();
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"delete-target.png",
			MediaType.IMAGE_PNG_VALUE,
			imageBytes
		);

		mockMvc.perform(multipart("/api/v1/auction/image")
				.file(file)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk());

		ProductImage savedImage = productImageRepository.findAll().stream()
			.filter(image -> image.getOriginalFileName().equals("delete-target.png"))
			.findFirst()
			.orElseThrow();

		mockMvc.perform(delete("/api/v1/auction/image/{imageId}", savedImage.getImageId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageId", is(savedImage.getImageId().intValue())))
			.andExpect(jsonPath("$.deleted").value(true));
	}

	@Test
	@DisplayName("카테고리 목록 조회는 계층 구조를 반환한다")
	void getCategoriesReturnsHierarchy() throws Exception {
		mockMvc.perform(get("/api/v1/auction/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)));
	}

	@Test
	@DisplayName("경매 등록 API는 REGULAR 판매 유형을 허용하지 않는다")
	void createAuctionFailsWhenSaleTypeIsRegular() throws Exception {
		mockMvc.perform(post("/api/v1/auction")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "MacBook Air M2",
					  "description": "Lightly used and includes charger.",
					  "saleType": "REGULAR",
					  "categoryId": 19,
					  "price": 1350000,
					  "startPrice": null,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "http://localhost:8080/uploads/auction/images/test-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "Seoul",
					  "draftId": null
					}
					""".formatted(uploadedImage.getImageId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_AUCTION_REQUEST"));
	}

	@Test
	@DisplayName("상위 카테고리로 상품 등록을 시도하면 검증 오류를 반환한다")
	void createAuctionFailsWhenCategoryIsNotLeaf() throws Exception {
		mockMvc.perform(post("/api/v1/auction")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "MacBook Air M2",
					  "description": "Lightly used and includes charger.",
					  "saleType": "AUCTION",
					  "categoryId": 5,
					  "price": null,
					  "startPrice": 500000,
					  "auctionDurationDays": 3,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "http://localhost:8080/uploads/auction/images/test-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "Seoul",
					  "draftId": null
					}
					""".formatted(uploadedImage.getImageId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_AUCTION_REQUEST"));
	}

	@Test
	@DisplayName("경매 판매 상품 등록 성공 시 즉시 AUCTION_LIVE 상태를 반환한다")
	void createAuctionLiveSuccess() throws Exception {
		mockMvc.perform(post("/api/v1/auction")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "Rolex Datejust",
					  "description": "Authentic watch in good condition.",
					  "saleType": "AUCTION",
					  "categoryId": 44,
					  "price": null,
					  "startPrice": 500000,
					  "auctionDurationDays": 3,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "http://localhost:8080/uploads/auction/images/test-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "Busan",
					  "draftId": null
					}
					""".formatted(uploadedImage.getImageId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.productId").isNumber())
			.andExpect(jsonPath("$.saleType").value("AUCTION"))
			.andExpect(jsonPath("$.status").value("AUCTION_LIVE"))
			.andExpect(jsonPath("$.auction.status").value("AUCTION_LIVE"))
			.andExpect(jsonPath("$.auction.startAt").value(notNullValue()))
			.andExpect(jsonPath("$.auction.endAt").value(notNullValue()));
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
