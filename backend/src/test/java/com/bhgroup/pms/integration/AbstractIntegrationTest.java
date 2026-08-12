package com.bhgroup.pms.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for tests that need real PostgreSQL behavior a mock or an
 * in-memory database can't reproduce: the GiST exclusion constraint that
 * blocks overbooking, Postgres enum types, and Flyway migrations actually
 * running end-to-end against a real database.
 *
 * The container is started once per JVM (static field) and shared across
 * all subclasses to keep the suite fast - Spring Boot's
 * {@code @ServiceConnection} wires the datasource automatically, no
 * manual JDBC URL/username/password plumbing needed. Requires Docker to
 * be available wherever these tests run (local machine, CI runner).
 *
 * Known local issue: on some Windows + Docker Desktop combinations, the
 * docker-java client used by Testcontainers gets an empty stub response
 * from Docker Desktop's internal gateway (BadRequestException, "Could
 * not find a valid Docker environment") even though the Docker CLI and
 * plain HTTP calls against the same endpoint work fine - confirmed not
 * fixable by switching named pipes, using TCP instead, or upgrading
 * Testcontainers. These tests fail to even start the Spring context on
 * such a machine ("Tests run: N, Errors: N" for every class extending
 * this one) while every other test suite is unaffected. This is a Docker
 * Desktop/docker-java compatibility gap, not a problem with the tests
 * or the constraint they verify (checked directly against a running
 * Postgres via psql - see the PR description). CI runs on Linux with a
 * standard Docker socket, where this does not occur.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.ContainerConfig.class)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }

    static class ContainerConfig {
        @org.springframework.context.annotation.Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return POSTGRES;
        }
    }
}
