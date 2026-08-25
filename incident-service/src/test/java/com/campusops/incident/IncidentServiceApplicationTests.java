package com.campusops.incident;

import com.campusops.incident.config.KafkaTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(KafkaTestConfig.class)
class IncidentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
