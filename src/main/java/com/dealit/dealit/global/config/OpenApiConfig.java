//Swagger/OpenAPI 문서의 제목, 설명, 버전, 서버 주소를 설정
package com.dealit.dealit.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI dealitOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Dealit API")
				.description("Dealit backend API documentation")
				.version("v1")
				.contact(new Contact().name("Dealit Team")))
			.components(new Components())
			.servers(List.of(
				new Server().url("http://localhost:8080").description("Local server")
			));
	}
}
