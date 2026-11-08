package com.pzhgp.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application Context Integration Tests")
class BackendApplicationTests {

	@Test
	@DisplayName("Should start the Spring application context")
	void contextLoads() {
	}
}
