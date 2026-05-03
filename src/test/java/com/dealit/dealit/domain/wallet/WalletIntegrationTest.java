package com.dealit.dealit.domain.wallet;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.repository.FcmTokenRepository;
import com.dealit.dealit.domain.wallet.repository.WalletLedgerRepository;
import com.dealit.dealit.domain.wallet.repository.WalletRepository;
import com.dealit.dealit.global.security.jwt.JwtService;
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
class WalletIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private FcmTokenRepository fcmTokenRepository;

	@Autowired
	private WalletRepository walletRepository;

	@Autowired
	private WalletLedgerRepository walletLedgerRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private String accessToken;

	@BeforeEach
	void setUp() {
		walletLedgerRepository.deleteAll();
		walletRepository.deleteAll();
		fcmTokenRepository.deleteAll();
		memberRepository.deleteAll();

		Member member = memberRepository.save(Member.create(
			"dealit-user",
			passwordEncoder.encode("Password123!"),
			"user@dealit.com",
			null,
			"홍길동"
		));
		member.assignDefaultNickname();
		memberRepository.save(member);
		accessToken = jwtService.generateAccessToken(member);
	}

	@Test
	@DisplayName("내 지갑을 조회하면 지갑이 없을 때 0원 지갑을 생성한다")
	void getMyWalletCreatesWallet() throws Exception {
		mockMvc.perform(get("/api/v1/wallet")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.walletId").isNumber())
			.andExpect(jsonPath("$.balance").value(0));

		assertThat(walletRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("딜릿머니를 충전하면 잔액이 증가하고 원장이 기록된다")
	void chargeWallet() throws Exception {
		mockMvc.perform(post("/api/v1/wallet/charge")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "amount": 30000
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value(30000));

		mockMvc.perform(get("/api/v1/wallet/ledgers")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].type").value("TEMP_CHARGE"))
			.andExpect(jsonPath("$.content[0].amount").value(30000))
			.andExpect(jsonPath("$.content[0].balanceAfter").value(30000));
	}

	@Test
	@DisplayName("환불은 잔액을 증가시키고 출금은 잔액을 차감한다")
	void refundAndWithdrawWallet() throws Exception {
		postAmount("/api/v1/wallet/charge", 50000)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value(50000));

		postAmount("/api/v1/wallet/refund", 10000)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value(60000));

		postAmount("/api/v1/wallet/withdraw", 25000)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value(35000));

		mockMvc.perform(get("/api/v1/wallet/ledgers")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].type").value("WITHDRAWAL"))
			.andExpect(jsonPath("$.content[0].amount").value(-25000))
			.andExpect(jsonPath("$.content[1].type").value("REFUND"))
			.andExpect(jsonPath("$.content[1].amount").value(10000));
	}

	@Test
	@DisplayName("잔액보다 큰 출금 요청은 실패한다")
	void withdrawFailsWhenBalanceInsufficient() throws Exception {
		postAmount("/api/v1/wallet/withdraw", 1)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_WALLET_REQUEST"))
			.andExpect(jsonPath("$.message").value("잔액이 부족합니다."));
	}

	@Test
	@DisplayName("0원 이하 금액은 검증 오류를 반환한다")
	void amountValidationFails() throws Exception {
		postAmount("/api/v1/wallet/charge", 0)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	private org.springframework.test.web.servlet.ResultActions postAmount(String url, long amount) throws Exception {
		return mockMvc.perform(post(url)
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "amount": %d
				}
				""".formatted(amount)));
	}
}
