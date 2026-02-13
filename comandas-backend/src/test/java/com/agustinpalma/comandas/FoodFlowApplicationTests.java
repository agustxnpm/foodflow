package com.agustinpalma.comandas;

import com.agustinpalma.comandas.infrastructure.config.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class FoodFlowApplicationTests {

	@Test
	void contextLoads() {
	}

}
