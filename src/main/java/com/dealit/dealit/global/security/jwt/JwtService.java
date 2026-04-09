package com.dealit.dealit.global.security.jwt;

import com.dealit.dealit.domain.member.entity.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

	private static final String LOGIN_ID_CLAIM = "loginId";
	private static final String ROLE_CLAIM = "role";

	private final SecretKey signingKey;
	private final long accessTokenExpirationMs;
	private final String issuer;

	public JwtService(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
		@Value("${jwt.issuer}") String issuer
	) {
		// HS256 서명 키는 충분한 길이의 secret 바이트로 생성한다.
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationMs = accessTokenExpirationMs;
		this.issuer = issuer;
	}

	public String generateAccessToken(Member member) {
		return generateAccessToken(member, Duration.ofMillis(accessTokenExpirationMs));
	}

	public String generateAccessToken(Member member, Duration validity) {
		Instant now = Instant.now();
		Instant expiration = now.plus(validity);

		// subject에는 memberId를, 커스텀 claim에는 로그인/권한 정보를 담아 토큰을 만든다.
		return Jwts.builder()
			.subject(String.valueOf(member.getMemberId()))
			.claim(LOGIN_ID_CLAIM, member.getLoginId())
			.claim(ROLE_CLAIM, "ROLE_USER")
			.issuer(issuer)
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiration))
			.signWith(signingKey)
			.compact();
	}

	public JwtClaims parse(String token) {
		// 서명/만료 검증이 실패하면 JJWT가 예외를 던진다.
		Claims claims = Jwts.parser()
			.verifyWith(signingKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();

		// 이후 필터/컨트롤러에서 쓰기 쉬운 도메인 형태로 변환한다.
		return new JwtClaims(
			Long.parseLong(claims.getSubject()),
			claims.get(LOGIN_ID_CLAIM, String.class),
			claims.get(ROLE_CLAIM, String.class)
		);
	}

	public long getAccessTokenExpirationMs() {
		return accessTokenExpirationMs;
	}
}
