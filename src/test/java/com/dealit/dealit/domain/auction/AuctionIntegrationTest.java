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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
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
	@DisplayName("내 판매중 경매 목록 조회는 현재 사용자가 등록한 진행중 경매를 페이징 응답으로 반환한다")
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
			.andExpect(jsonPath("$.content[0].description").value("Includes dock and controller."))
			.andExpect(jsonPath("$.content[0].categoryName").value("사무용노트북"))
			.andExpect(jsonPath("$.content[0].thumbnailUrl").value("http://localhost:8080/uploads/auction/images/test-image.jpg"))
			.andExpect(jsonPath("$.content[0].auctionStatus").value("AUCTION_LIVE"))
			.andExpect(jsonPath("$.content[0].startPrice").value(180000))
			.andExpect(jsonPath("$.content[0].minimumBidAmount").value(1800))
			.andExpect(jsonPath("$.content[0].currentPrice").value(180000))
			.andExpect(jsonPath("$.content[0].minimumNextBidPrice").value(181800))
			.andExpect(jsonPath("$.content[0].bidCount").value(0))
			.andExpect(jsonPath("$.content[0].bidderCount").value(0))
			.andExpect(jsonPath("$.content[0].startAt").value(notNullValue()))
			.andExpect(jsonPath("$.content[0].endAt").value(notNullValue()))
			.andExpect(jsonPath("$.content[0].canEdit").value(true))
			.andExpect(jsonPath("$.content[0].canDelete").value(true))
			.andExpect(jsonPath("$.content[0].createdAt").value(notNullValue()))
			.andExpect(jsonPath("$.content[0].updatedAt").value(notNullValue()))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	@DisplayName("이미지 업로드 응답은 브라우저에서 접근 가능한 절대 URL을 반환하고 정적 서빙된다")
	void uploadAuctionImageReturnsAccessibleUrl() throws Exception {
		byte[] imageBytes = "test-image".getBytes();
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"스크린샷 2026-04-14 오전 11.28.49.png",
			MediaType.IMAGE_PNG_VALUE,
			imageBytes
		);

		mockMvc.perform(multipart("/api/v1/auction/image")
				.file(file)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageId").isNumber())
			.andExpect(jsonPath("$.imageUrl").value(startsWith("http://localhost:8080/uploads/auction/images/")))
			.andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.endsWith(".png")))
			.andReturn();

		ProductImage savedImage = productImageRepository.findAll().stream()
			.filter(image -> image.getOriginalFileName().equals("스크린샷 2026-04-14 오전 11.28.49.png"))
			.findFirst()
			.orElseThrow();

		mockMvc.perform(get(savedImage.getImageUrl()))
			.andExpect(status().isOk())
			.andExpect(content().bytes(imageBytes));
	}

	@Test
	@DisplayName("임시 업로드 이미지는 imageId 기준으로 삭제할 수 있다")
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

		mockMvc.perform(delete("/api/v1/auction/image/{imageId}", savedImage.getImageId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AUCTION_IMAGE_NOT_FOUND"));

		Path deletedFilePath = Path.of(imageStorageRoot)
			.resolve(savedImage.getImageUrl().replaceFirst("^/uploads/", ""));
		org.assertj.core.api.Assertions.assertThat(Files.exists(deletedFilePath)).isFalse();
	}

	@Test
	@DisplayName("경매 수정 중 기존 상품 이미지를 imageId 기준으로 삭제할 수 있다")
	void deleteLinkedAuctionImageSuccess() throws Exception {
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

		Long auctionId = auctionRepository.findAll().getFirst().getAuctionId();

		mockMvc.perform(delete("/api/v1/auction/image/{imageId}", uploadedImage.getImageId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageId", is(uploadedImage.getImageId().intValue())))
			.andExpect(jsonPath("$.deleted").value(true));

		mockMvc.perform(get("/api/v1/auctions/{auctionId}/edit", auctionId)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.images", hasSize(0)));

		ProductImage deletedImage = productImageRepository.findById(uploadedImage.getImageId()).orElseThrow();
		org.assertj.core.api.Assertions.assertThat(deletedImage.getDeletedAt()).isNotNull();
		org.assertj.core.api.Assertions.assertThat(deletedImage.getProduct()).isNull();
	}

	@Test
	@DisplayName("카테고리 목록 조회는 계층형 트리 구조를 반환한다")
	void getCategoriesReturnsHierarchy() throws Exception {
		mockMvc.perform(get("/api/v1/auction/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].nameKo").value("전자기기"))
			.andExpect(jsonPath("$[0].children", hasSize(4)))
			.andExpect(jsonPath("$[0].children[0].nameKo").value("스마트폰"))
			.andExpect(jsonPath("$[0].children[0].children", hasSize(3)))
			.andExpect(jsonPath("$[0].children[0].children[0].nameKo").value("아이폰"));
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
			.andExpect(jsonPath("$.code").value("INVALID_AUCTION_REQUEST"))
			.andExpect(jsonPath("$.message").value("경매 등록에서는 AUCTION 판매 유형만 허용됩니다."));
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
			.andExpect(jsonPath("$.code").value("INVALID_AUCTION_REQUEST"))
			.andExpect(jsonPath("$.message").value("최하위 카테고리만 선택할 수 있습니다."));
	}

	@Test
	@DisplayName("경매 판매 상품 등록에 성공하면 즉시 AUCTION_LIVE 상태와 계산된 시간을 반환한다")
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

	@Test
	@DisplayName("경매 등록은 테스트용 소수 day 기간으로 10초 경매를 생성할 수 있다")
	void createAuctionSupportsFractionalDurationDaysForTenSecondAuction() throws Exception {
		mockMvc.perform(post("/api/v1/auction")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "10 second auction",
					  "description": "Short auction for completion test.",
					  "saleType": "AUCTION",
					  "categoryId": 21,
					  "price": null,
					  "startPrice": 500000,
					  "minimumBidAmount": 5000,
					  "auctionDurationDays": 0.00011574074074074075,
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
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.auction.startAt").value(notNullValue()))
			.andExpect(jsonPath("$.auction.endAt").value(notNullValue()));

		var auction = auctionRepository.findAll().getFirst();
		assertThat(Duration.between(auction.getAuctionStartAt(), auction.getAuctionEndAt()))
			.isEqualTo(Duration.ofSeconds(10));
	}

	@Test
	@DisplayName("경매 상세 조회는 상품, 판매자, 이미지, 입찰 메타 정보를 함께 반환한다")
	void getAuctionDetailReturnsProductSellerImagesAndBidMeta() throws Exception {
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
					  "minimumBidAmount": 1800,
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

		Long auctionId = auctionRepository.findAll().getFirst().getAuctionId();

		mockMvc.perform(get("/api/v1/auctions/{auctionId}", auctionId)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.auctionId").value(auctionId))
			.andExpect(jsonPath("$.productId").isNumber())
			.andExpect(jsonPath("$.name").value("Nintendo Switch OLED"))
			.andExpect(jsonPath("$.description").value("Includes dock and controller."))
			.andExpect(jsonPath("$.categoryId").value(21))
			.andExpect(jsonPath("$.categoryName").value("사무용노트북"))
			.andExpect(jsonPath("$.location").value("서울 마포구"))
			.andExpect(jsonPath("$.images", hasSize(1)))
			.andExpect(jsonPath("$.images[0].imageId").value(uploadedImage.getImageId()))
			.andExpect(jsonPath("$.images[0].imageUrl").value("http://localhost:8080/uploads/auction/images/test-image.jpg"))
			.andExpect(jsonPath("$.images[0].sortOrder").value(1))
			.andExpect(jsonPath("$.seller.memberId").isNumber())
			.andExpect(jsonPath("$.seller.nickname").value(startsWith("Dealit#")))
			.andExpect(jsonPath("$.startPrice").value(180000))
			.andExpect(jsonPath("$.currentPrice").value(180000))
			.andExpect(jsonPath("$.minimumBidAmount").value(1800))
			.andExpect(jsonPath("$.minimumNextBidPrice").value(181800))
			.andExpect(jsonPath("$.bidCount").value(0))
			.andExpect(jsonPath("$.bidderCount").value(0))
			.andExpect(jsonPath("$.startAt").value(notNullValue()))
			.andExpect(jsonPath("$.endsAt").value(notNullValue()))
			.andExpect(jsonPath("$.serverTime").value(notNullValue()))
			.andExpect(jsonPath("$.status").value("AUCTION_LIVE"));
	}

	@Test
	@DisplayName("입찰가는 현재가에 최소 입찰 금액을 더한 금액 이상이어야 한다")
	void bidFailsWhenBidPriceIsBelowMinimumNextBidPrice() throws Exception {
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
					  "minimumBidAmount": 1800,
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

		Long auctionId = auctionRepository.findAll().getFirst().getAuctionId();
		Member bidder = Member.create(
			"auction-bidder",
			passwordEncoder.encode("Password123!"),
			"auction-bidder@dealit.com",
			null,
			"입찰자"
		);
		Member savedBidder = memberRepository.save(bidder);
		savedBidder.assignDefaultNickname();
		String bidderToken = jwtService.generateAccessToken(memberRepository.save(savedBidder));

		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
				.header("Authorization", "Bearer " + bidderToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "bidPrice": 181000
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_AUCTION_REQUEST"))
			.andExpect(jsonPath("$.message").value("최소 입찰 금액을 충족해야 합니다."));
	}

	@Test
	@DisplayName("경매 판매에서 진행 기간이 없으면 비즈니스 검증 오류를 반환한다")
	void createAuctionFailsWhenDurationMissing() throws Exception {
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
					  "auctionDurationDays": null,
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
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_AUCTION_REQUEST"))
			.andExpect(jsonPath("$.message").value("경매 판매에서는 진행 기간이 필수입니다."));
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
