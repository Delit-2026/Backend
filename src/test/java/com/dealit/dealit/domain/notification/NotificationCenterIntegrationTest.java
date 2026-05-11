package com.dealit.dealit.domain.notification;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.dto.NotificationCreateRequest;
import com.dealit.dealit.domain.notification.entity.InAppNotificationType;
import com.dealit.dealit.domain.notification.service.NotificationCenterService;
import com.dealit.dealit.global.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationCenterIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private NotificationCenterService notificationCenterService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private Member member;
	private String accessToken;

	@BeforeEach
	void setUp() {
		member = memberRepository.save(Member.create(
			"notification-user",
			passwordEncoder.encode("Password123!"),
			"notification@dealit.com",
			null,
			"알림유저"
		));
		member.assignDefaultNickname();
		memberRepository.save(member);
		accessToken = jwtService.generateAccessToken(member);
	}

	@Test
	@DisplayName("내 알림 목록을 최신순으로 조회한다")
	void getMyNotifications() throws Exception {
		createNotification("거래 요청", "새 거래 요청이 도착했습니다.", "DEAL", 10L);
		createNotification("상품 찜", "상품에 찜이 추가되었습니다.", "PRODUCT", 20L);

		mockMvc.perform(get("/api/v1/notifications")
				.header("Authorization", "Bearer " + accessToken)
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].title").value("상품 찜"))
			.andExpect(jsonPath("$.content[0].read").value(false))
			.andExpect(jsonPath("$.content[0].targetType").value("PRODUCT"))
			.andExpect(jsonPath("$.content[0].targetId").value(20))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	@DisplayName("안 읽은 알림 개수를 조회하고 읽음 처리한다")
	void readNotifications() throws Exception {
		Long notificationId = createNotification("거래 요청", "새 거래 요청이 도착했습니다.", "DEAL", 10L);
		createNotification("상품 찜", "상품에 찜이 추가되었습니다.", "PRODUCT", 20L);

		mockMvc.perform(get("/api/v1/notifications/unread-count")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.count").value(2));

		mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", notificationId)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.notificationId").value(notificationId))
			.andExpect(jsonPath("$.read").value(true));

		mockMvc.perform(get("/api/v1/notifications/unread-count")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.count").value(1));

		mockMvc.perform(patch("/api/v1/notifications/read-all")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.updatedCount").value(1));

		mockMvc.perform(get("/api/v1/notifications/unread-count")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.count").value(0));
	}

	@Test
	@DisplayName("타입별 안 읽은 알림 개수를 조회한다")
	void getUnreadCountsByType() throws Exception {
		createNotification(InAppNotificationType.PRODUCT, "상품 찜", "상품에 찜이 추가되었습니다.", "PRODUCT", 20L);
		createNotification(InAppNotificationType.PRODUCT, "상품 찜", "상품에 찜이 추가되었습니다.", "PRODUCT", 21L);
		createNotification(InAppNotificationType.AUCTION, "첫 입찰 발생", "새 입찰이 들어왔습니다.", "AUCTION", 30L);

		mockMvc.perform(get("/api/v1/notifications/unread-counts-by-type")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].type", containsInAnyOrder("PRODUCT", "AUCTION")))
			.andExpect(jsonPath("$[*].count", containsInAnyOrder(2, 1)));
	}

	@Test
	@DisplayName("알림을 삭제하면 목록과 안 읽은 개수에서 제외된다")
	void deleteNotification() throws Exception {
		Long notificationId = createNotification("거래 요청", "새 거래 요청이 도착했습니다.", "DEAL", 10L);
		createNotification("상품 찜", "상품에 찜이 추가되었습니다.", "PRODUCT", 20L);

		mockMvc.perform(delete("/api/v1/notifications/{notificationId}", notificationId)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.notificationId").value(notificationId))
			.andExpect(jsonPath("$.deleted").value(true));

		mockMvc.perform(get("/api/v1/notifications")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].title").value("상품 찜"))
			.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(get("/api/v1/notifications/unread-count")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.count").value(1));
	}

	private Long createNotification(String title, String content, String targetType, Long targetId) {
		return createNotification(InAppNotificationType.TRADE, title, content, targetType, targetId);
	}

	private Long createNotification(
		InAppNotificationType type,
		String title,
		String content,
		String targetType,
		Long targetId
	) {
		return notificationCenterService.create(
			member.getMemberId(),
			new NotificationCreateRequest(
				type,
				title,
				content,
				targetType,
				targetId,
				null
			)
		).notificationId();
	}
}
