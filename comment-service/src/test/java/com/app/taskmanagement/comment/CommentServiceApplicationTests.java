package com.app.taskmanagement.comment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 *
 * @TestPropertySource overrides the datasource so this test doesn't need a live
 *                     MySQL instance to run (CI/CD environments without a DB
 *                     will still pass).
 *
 *                     For full integration tests, use @DataJpaTest with an H2
 *                     in-memory DB, or spin up MySQL via Testcontainers.
 */
@SpringBootTest
@TestPropertySource(properties = { "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
		"spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false" })
class CommentServiceApplicationTests {

	@Test
	void contextLoads() {
		// If this passes, the entire Spring context (beans, security, JPA) wired
		// correctly
	}
}