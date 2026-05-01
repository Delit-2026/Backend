package com.dealit.dealit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.profiles.active=test",
	"spring.flyway.enabled=false",
	"app.firebase.enabled=false"
})
class DealitApplicationTests {

	@Test
	void contextLoads() {
	}

}
