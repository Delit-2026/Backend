//스프링 시큐리티의 전체 HTTP 보안 정책을 정의
//현재는 CSRF, 폼로그인, Basic 인증 비활성화 후 일부 경로만 허용하고 나머지는 인증 필요로 막습니다.
// 기본 뼈대로는 올바르게 작성됨.
// 다만 현재 상태는 인증 필터(JWT 등) 설정이 없어서, 허용 경로 외 API는 사실상 접근 불가가 될 수 있습니다.
// 즉, 보안 정책 자체는 맞지만 인증 방식 구현이 아직 붙지 않은 상태

package com.dealit.dealit.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/",
					"/error",
					"/api/v1/health",
					"/api/v1/members/signup",
					"/swagger-ui.html",
					"/swagger-ui/**",
					"/api-docs/**",
					"/actuator/health",
					"/actuator/info"
				).permitAll()
				.anyRequest().authenticated()
			);

		return http.build();
	}
}
