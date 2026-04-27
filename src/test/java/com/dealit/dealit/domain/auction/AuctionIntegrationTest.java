package com.dealit.dealit.domain.auction;

import com.dealit.dealit.domain.auction.entity.AuctionProductImage;
import com.dealit.dealit.domain.auction.repository.AuctionProductImageRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.hamcrest.Matchers.containsString;
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
	private AuctionProductImageRepository auctionProductImageRepository;

	@Value("${app.images.storage-root}")
	private String imageStorageRoot;

	private AuctionProductImage uploadedImage;

	@BeforeEach
	void setUp() throws IOException {
		auctionProductImageRepository.deleteAll();
		deleteStoredImages();
		uploadedImage = auctionProductImageRepository.save(
			AuctionProductImage.createTemporary(
				"/auction/images/test-image.jpg",
				"test-image.jpg"
			)
		);
	}

	@AfterEach
	void tearDown() throws IOException {
		deleteStoredImages();
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

		mockMvc.perform(multipart("/api/v1/auction/image").file(file))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageId").isNumber())
			.andExpect(jsonPath("$.imageUrl").value(startsWith("http://localhost:8080/auction/images/")))
			.andExpect(jsonPath("$.imageUrl").value(containsString("%EC%8A%A4%ED%81%AC")))
			.andReturn();

		AuctionProductImage savedImage = auctionProductImageRepository.findAll().stream()
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

		mockMvc.perform(multipart("/api/v1/auction/image").file(file))
			.andExpect(status().isOk());

		AuctionProductImage savedImage = auctionProductImageRepository.findAll().stream()
			.filter(image -> image.getOriginalFileName().equals("delete-target.png"))
			.findFirst()
			.orElseThrow();

		mockMvc.perform(delete("/api/v1/auction/image/{imageId}", savedImage.getImageId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageId", is(savedImage.getImageId().intValue())))
			.andExpect(jsonPath("$.deleted").value(true));

		mockMvc.perform(delete("/api/v1/auction/image/{imageId}", savedImage.getImageId()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AUCTION_IMAGE_NOT_FOUND"));

		Path deletedFilePath = Path.of(imageStorageRoot)
			.resolve(savedImage.getImageUrl().replaceFirst("^/auction/images/", "auction/images/"));
		org.assertj.core.api.Assertions.assertThat(Files.exists(deletedFilePath)).isFalse();
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
					      "imageUrl": "http://localhost:8080/auction/images/test-image.jpg",
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
					      "imageUrl": "http://localhost:8080/auction/images/test-image.jpg",
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
					      "imageUrl": "http://localhost:8080/auction/images/test-image.jpg",
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
	@DisplayName("경매 판매에서 진행 기간이 없으면 비즈니스 검증 오류를 반환한다")
	void createAuctionFailsWhenDurationMissing() throws Exception {
		mockMvc.perform(post("/api/v1/auction")
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
					      "imageUrl": "http://localhost:8080/auction/images/test-image.jpg",
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
