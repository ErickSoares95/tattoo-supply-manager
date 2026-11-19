package com.ericksoares.tattoo.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * T3: every Spring test context points at one throwaway Postgres (same pgvector image as
 * production and docker-compose), started once per test JVM and removed by Testcontainers at
 * the end - no local database to keep running, and CI no longer needs a Postgres service.
 * Registered for all contexts in META-INF/spring.factories.
 *
 * <p>Without Docker, set {@code TSM_TEST_DB_URL} (plus {@code TSM_TEST_DB_USER} /
 * {@code TSM_TEST_DB_PASSWORD}) to run against an existing database instead.</p>
 */
public class PostgresTestContainer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static PostgreSQLContainer<?> container;

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String externalUrl = System.getenv("TSM_TEST_DB_URL");
        Map<String, Object> properties = externalUrl != null
                ? Map.of(
                        "spring.datasource.url", externalUrl,
                        "spring.datasource.username", envOr("TSM_TEST_DB_USER", "tattoo_user"),
                        "spring.datasource.password", envOr("TSM_TEST_DB_PASSWORD", "tattoo_pass"))
                : Map.of(
                        "spring.datasource.url", start().getJdbcUrl(),
                        "spring.datasource.username", container.getUsername(),
                        "spring.datasource.password", container.getPassword());

        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("postgresTestContainer", properties));
    }

    private static synchronized PostgreSQLContainer<?> start() {
        if (container == null) {
            container = new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("tattoo-supply-manager-test")
                    .withUsername("tattoo_user")
                    .withPassword("tattoo_pass");
            container.start();
        }
        return container;
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
