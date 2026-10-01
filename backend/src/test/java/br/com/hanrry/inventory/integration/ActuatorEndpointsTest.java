package br.com.hanrry.inventory.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0"
)
@ActiveProfiles("test")
@Testcontainers
class ActuatorEndpointsTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    int serverPort;

    @LocalManagementPort
    int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    private final RestTemplate rawRestTemplate = restTemplateWithoutErrorHandler();

    private static RestTemplate restTemplateWithoutErrorHandler() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return restTemplate;
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("management.server.port", () -> 0);
    }

    @Test
    void shouldExposeHealthAndPrometheusOnManagementPortOnly() {
        assertTrue(managementPort > 0);
        assertTrue(serverPort != managementPort);

        ResponseEntity<String> health = rawRestTemplate.getForEntity(
                "http://localhost:" + managementPort + "/actuator/health",
                String.class
        );
        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertTrue(health.getBody() != null && health.getBody().contains("\"status\":\"UP\""));

        ResponseEntity<String> prometheus = rawRestTemplate.getForEntity(
                "http://localhost:" + managementPort + "/actuator/prometheus",
                String.class
        );
        assertEquals(HttpStatus.OK, prometheus.getStatusCode());
        assertTrue(prometheus.getBody() != null && prometheus.getBody().contains("jvm_memory"));

        ResponseEntity<String> prometheusOnApi = restTemplate.getForEntity(
                "/actuator/prometheus",
                String.class
        );
        assertTrue(
                prometheusOnApi.getStatusCode().is4xxClientError(),
                () -> "prometheus na porta da API deveria ser 4xx, veio "
                        + prometheusOnApi.getStatusCode()
        );

        ResponseEntity<String> env = rawRestTemplate.getForEntity(
                "http://localhost:" + managementPort + "/actuator/env",
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, env.getStatusCode());

        ResponseEntity<String> heapdump = rawRestTemplate.getForEntity(
                "http://localhost:" + managementPort + "/actuator/heapdump",
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, heapdump.getStatusCode());
    }

    @Test
    void shouldKeepJwtOnBusinessApi() {
        ResponseEntity<String> me = restTemplate.getForEntity(
                "http://localhost:" + serverPort + "/api/v1/users/me",
                String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, me.getStatusCode());
        assertFalse(me.getStatusCode().is2xxSuccessful());
    }
}
