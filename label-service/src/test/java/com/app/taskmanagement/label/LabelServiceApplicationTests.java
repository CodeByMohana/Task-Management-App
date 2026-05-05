package com.app.taskmanagement.label;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies the entire Spring application context starts up
 * correctly.
 *
 * WHAT IS A SMOKE TEST? It's the most basic test: "does the app even start
 * without crashing?" If this test passes, it means: - All @Bean definitions are
 * valid - All @Autowired dependencies are satisfied (no missing beans) - JPA
 * entity mapping is correct (no @Column mismatches, etc.) - Security config is
 * valid
 *
 * @TestPropertySource overrides the database URL so this test uses H2 (an
 *                     in-memory database) instead of MySQL. This means: - The
 *                     test runs without a running MySQL server. - CI/CD
 *                     pipelines (GitHub Actions, etc.) can run this test with
 *                     no DB setup. - The test is fast — H2 starts and shuts
 *                     down in milliseconds.
 *
 *                     FOR FULL INTEGRATION TESTS: Use @DataJpaTest with H2 to
 *                     test repositories in isolation. Use Testcontainers to
 *                     spin up a real MySQL instance in tests.
 */
@SpringBootTest
@TestPropertySource(properties = {
		// Use H2 in-memory database instead of MySQL for tests
		"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
		"spring.datasource.password=",
		// create-drop: creates tables at test start, drops them when test ends
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
		// Disable Eureka so the test doesn't need a running Eureka server
		"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false" })
class LabelServiceApplicationTests {

	@Test
	void contextLoads() {
		/*
		 * No assertions needed here. If the Spring context fails to start (missing
		 * bean, config error, etc.), Spring will throw an exception and this test will
		 * fail automatically.
		 *
		 * A passing test = the whole application wired up correctly.
		 */
	}
}