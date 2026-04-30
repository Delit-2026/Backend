package com.dealit.dealit.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dealit.dealit.domain.chat.service.ProductOwnershipPort;
import com.dealit.dealit.domain.chat.service.ProductSummaryPort;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class EventStreamIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ProductOwnershipPort productOwnershipPort;

    @MockitoBean
    private ProductSummaryPort productSummaryPort;

    private String sellerAccessToken;
    private String buyerAccessToken;
    private Long sellerId;
    private Long buyerId;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();

        Member seller = memberRepository.saveAndFlush(Member.create(
                "sse-seller",
                passwordEncoder.encode("Password123!"),
                "sse-seller@dealit.com",
                null,
                "seller"
        ));
        seller.assignDefaultNickname();
        seller = memberRepository.saveAndFlush(seller);

        Member buyer = memberRepository.saveAndFlush(Member.create(
                "sse-buyer",
                passwordEncoder.encode("Password123!"),
                "sse-buyer@dealit.com",
                null,
                "buyer"
        ));
        buyer.assignDefaultNickname();
        buyer = memberRepository.saveAndFlush(buyer);

        sellerId = seller.getMemberId();
        buyerId = buyer.getMemberId();
        sellerAccessToken = jwtService.generateAccessToken(seller);
        buyerAccessToken = jwtService.generateAccessToken(buyer);
    }

    @Test
    @DisplayName("전역 SSE 구독 시 연결 이벤트를 즉시 수신한다")
    void subscribe_emitsConnectedEvent() throws Exception {
        MvcResult streamResult = mockMvc.perform(get("/api/v1/events/stream")
                        .header("Authorization", "Bearer " + sellerAccessToken))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = waitForContent(streamResult, "event.stream.connected");
        assertThat(body).contains("event:event.stream.connected");
        assertThat(body).contains("\"type\":\"event.stream.connected\"");
        assertThat(body).contains("\"userId\":" + sellerId);
    }

    @Test
    @DisplayName("채팅 전용 SSE 경로는 제공하지 않는다")
    void chatScopedStreamIsNotProvided() throws Exception {
        mockMvc.perform(get("/api/v1/chats/stream")
                        .header("Authorization", "Bearer " + sellerAccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("메시지 전송 후 전역 SSE 구독 중인 사용자는 채팅방 업데이트와 안읽음 개수 이벤트를 받는다")
    void sendMessage_emitsRoomAndUnreadEvents() throws Exception {
        assertMessageSendEmitsRoomAndUnreadEvents();
    }

    private void assertMessageSendEmitsRoomAndUnreadEvents() throws Exception {
        Long productId = 1000L;
        when(productOwnershipPort.getOwnerIdByProductId(productId)).thenReturn(sellerId);
        when(productSummaryPort.getSummaryByProductId(productId))
                .thenReturn(new ProductSummaryPort.ProductSummary(productId, "sse-product", null));

        String createRoomResponse = mockMvc.perform(post("/api/v1/chats/rooms")
                        .header("Authorization", "Bearer " + sellerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "receiverId": %d
                                }
                                """.formatted(productId, buyerId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long roomId = extractLong(createRoomResponse, "\"roomId\":");

        MvcResult streamResult = mockMvc.perform(get("/api/v1/events/stream")
                        .header("Authorization", "Bearer " + sellerAccessToken))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        waitForContent(streamResult, ".stream.connected");

        mockMvc.perform(post("/api/v1/chats/rooms/{roomId}/messages", roomId)
                        .header("Authorization", "Bearer " + buyerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageType": "TEXT",
                                  "content": "hello from buyer"
                                }
                                """))
                .andExpect(status().isCreated());

        String body = waitForContent(streamResult, "chat.unread-count.updated");
        assertThat(body).contains("event:chat.room.updated");
        assertThat(body).contains("\"type\":\"chat.room.updated\"");
        assertThat(body).contains("\"roomId\":" + roomId);
        assertThat(body).contains("\"content\":\"hello from buyer\"");
        assertThat(body).contains("\"unreadCount\":1");
        assertThat(body).contains("event:chat.unread-count.updated");
        assertThat(body).contains("\"type\":\"chat.unread-count.updated\"");
        assertThat(body).contains("\"totalUnreadCount\":1");
    }

    private String waitForContent(MvcResult result, String token) throws Exception {
        for (int i = 0; i < 20; i++) {
            String body = result.getResponse().getContentAsString();
            if (body.contains(token)) {
                return body;
            }
            Thread.sleep(100);
        }
        return result.getResponse().getContentAsString();
    }

    private Long extractLong(String json, String marker) {
        int start = json.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        int valueStart = start + marker.length();
        int valueEnd = valueStart;
        while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
            valueEnd++;
        }
        return Long.parseLong(json.substring(valueStart, valueEnd));
    }
}
