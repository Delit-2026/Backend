package com.dealit.dealit.global.security.jwt;

// 헤더 파싱 시 문자열 하드코딩을 피하기 위한 상수 모음
public final class JwtConstants {

	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";

	private JwtConstants() {
	}
}
