package com.dealit.dealit.global.config;

import com.dealit.dealit.global.security.jwt.JwtAuthenticationEntryPoint;
import com.dealit.dealit.global.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	public SecurityConfig(
		JwtAuthenticationFilter jwtAuthenticationFilter,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
	) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/",
					"/error",
					"/auction/images/**",
					"/product/images/**",
					"/profile/images/**",
					"/api/v1/health",
					"/api/v1/members/signup",
					"/api/v1/members/login-id/check",
					"/api/v1/members/nickname/check",
					"/api/v1/email/verification/send",
					"/api/v1/email/verification/confirm",
					"/api/v1/auth/login",
					"/api/v1/locations/resolve",
					"/api/v1/auction/**",
					"/swagger-ui.html",
					"/swagger-ui/**",
						"/api-docs",
						"/api-docs.yaml",
						"/api-docs/**",
					"/actuator/health",
					"/actuator/info"
				).permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/products/image").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/v1/products/image/*").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/v1/products/categories").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/products/category/recommend").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/products/price/recommend").permitAll()
				.anyRequest().authenticated()
			);

		return http.build();
	}
}
