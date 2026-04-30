package com.dealit.dealit.domain.chat;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dealit.dealit.domain.chat.exception.ProductNotFoundException;
import com.dealit.dealit.domain.chat.service.ProductOwnershipPort;
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

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerStrictPolicyIntegrationTest {

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

    private String accessToken;
    private Long receiverId;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();

        Member member = Member.create(
                "chat-user",
                passwordEncoder.encode("Password123!"),
                "chat-user@dealit.com",
                null,
                "채팅유저"
        );
        Member savedMember = memberRepository.saveAndFlush(member);
        savedMember.assignDefaultNickname();
        savedMember = memberRepository.saveAndFlush(savedMember);

        Member receiver = Member.create(
                "chat-receiver",
                passwordEncoder.encode("Password123!"),
                "chat-receiver@dealit.com",
                null,
                "수신자"
        );
        Member savedReceiver = memberRepository.saveAndFlush(receiver);
        savedReceiver.assignDefaultNickname();
        savedReceiver = memberRepository.saveAndFlush(savedReceiver);

        accessToken = jwtService.generateAccessToken(savedMember);
        receiverId = savedReceiver.getMemberId();
    }

    @Test
    @DisplayName("strict 정책: 상품 미존재면 채팅방 생성은 404/PRODUCT_NOT_FOUND")
    void createRoom_returns404_whenProductNotFound() throws Exception {
        when(productOwnershipPort.getOwnerIdByProductId(999999L))
                .thenThrow(new ProductNotFoundException("유효한 상품을 찾을 수 없습니다. productId=999999"));

        mockMvc.perform(post("/api/v1/chats/rooms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 999999,
                                  "receiverId": %d
                                }
                                """.formatted(receiverId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }
}