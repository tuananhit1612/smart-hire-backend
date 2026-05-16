package com.smarthire.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "app.seed.enabled=false")
@ActiveProfiles("test")
class SmartHireBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
