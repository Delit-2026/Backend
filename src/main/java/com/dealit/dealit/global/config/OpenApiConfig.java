//Swagger/OpenAPI 문서의 제목, 설명, 버전, 서버 주소를 설정
package com.dealit.dealit.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI dealitOpenAPI() {
		SecurityScheme bearerAuthScheme = new SecurityScheme()
			.name("Authorization")
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT")
			.in(SecurityScheme.In.HEADER);

		return new OpenAPI()
			.info(new Info()
				.title("Dealit API")
				.description("Dealit backend API documentation")
				.version("v1")
				.contact(new Contact().name("Dealit Team")))
			.components(new Components().addSecuritySchemes("bearerAuth", bearerAuthScheme))
			.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
			.servers(List.of(
				new Server().url("http://localhost:8080").description("Local server")
			));
	}
}
