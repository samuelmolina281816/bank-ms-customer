package com.nttdata.bank_ms_customer.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerPostgresIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("customer_test")
                    .withUsername("customer_test")
                    .withPassword("customer_test");

    @LocalServerPort
    private int port;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private DatabaseClient databaseClient;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.config.enabled", () -> false);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.separator", () -> "@@");
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getFirstMappedPort() + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    @Test
    void customerLifecycleUsesGeneratedContractAndPostgresPersistence() {
        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
        CustomerResponse created = webTestClient.post()
                .uri("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "customerType": "PERSONAL",
                          "documentType": "DNI",
                          "documentNumber": "00123456",
                          "firstName": "Ana",
                          "lastName": "Lopez",
                          "email": "ana@example.com",
                          "phone": "5551234"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("X-Correlation-Id")
                .expectBody(CustomerResponse.class)
                .returnResult()
                .getResponseBody();

        assertTrue(created != null);
        assertEquals(10_000_001L, created.getCustomerNumber());

        List<Long> concurrentlyGeneratedNumbers = Flux.range(0, 20)
                .flatMap(index -> webClient.post()
                        .uri("/api/v1/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("""
                                {
                                  "customerType": "PERSONAL",
                                  "documentType": "DNI",
                                  "documentNumber": "%s",
                                  "firstName": "Test",
                                  "lastName": "Customer",
                                  "email": "customer%s@example.com",
                                  "phone": "5551234"
                                }
                                """.formatted(String.format(Locale.ROOT, "%08d", index + 10_000_000),
                                index))
                        .retrieve()
                        .bodyToMono(CustomerResponse.class)
                        .map(CustomerResponse::getCustomerNumber))
                .collectList()
                .block(Duration.ofSeconds(30));
        assertTrue(concurrentlyGeneratedNumbers != null);
        assertEquals(20, concurrentlyGeneratedNumbers.stream().distinct().count());

        List<Integer> duplicateRequestStatuses = Flux.range(0, 2)
                .flatMap(index -> webClient.post()
                        .uri("/api/v1/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("""
                                {
                                  "customerType": "PERSONAL",
                                  "documentType": "DNI",
                                  "documentNumber": "12349876",
                                  "firstName": "Race",
                                  "lastName": "Condition",
                                  "email": "race%s@example.com",
                                  "phone": "5551234"
                                }
                                """.formatted(index))
                        .exchangeToMono(response -> response.releaseBody()
                                .thenReturn(response.statusCode().value())))
                .collectList()
                .block(Duration.ofSeconds(30));
        assertTrue(duplicateRequestStatuses != null);
        assertEquals(1, duplicateRequestStatuses.stream().filter(status -> status == 201).count());
        assertEquals(1, duplicateRequestStatuses.stream().filter(status -> status == 409).count());

        webTestClient.get()
                .uri("/api/v1/customer")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();

        webTestClient.get()
                .uri("/api/v1/customer/{customerNumber}", created.getCustomerNumber())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.documentNumber").isEqualTo("00123456");

        webTestClient.get()
                .uri("/api/v1/customer/{customerNumber}", 99_999_999L)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_NOT_FOUND");

        webTestClient.get()
                .uri("/api/v1/customer/documents/DNI/00123456")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.customerNumber").isEqualTo(10_000_001);

        webTestClient.post()
                .uri("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "customerType": "PERSONAL",
                          "documentType": "DNI",
                          "documentNumber": "00123456",
                          "firstName": "Ana",
                          "lastName": "Lopez",
                          "email": "ana@example.com",
                          "phone": "5551234"
                        }
                        """)
                .exchange()
                .expectStatus().isEqualTo(409);

        webTestClient.put()
                .uri("/api/v1/customer/{customerNumber}", created.getCustomerNumber())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"ana.updated@example.com"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("ana.updated@example.com");

        webTestClient.get()
                .uri("/api/v1/customer/documents/DNI/00123456/eligibility")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.eligible").isEqualTo(true);

        webTestClient.delete()
                .uri("/api/v1/customer/{customerNumber}", created.getCustomerNumber())
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/v1/customer/documents/DNI/00123456/eligibility")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.eligible").isEqualTo(false);

        webTestClient.delete()
                .uri("/api/v1/customer/{customerNumber}", created.getCustomerNumber())
                .exchange()
                .expectStatus().isNoContent();

        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        ResourceDatabasePopulator schemaPopulator = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
        schemaPopulator.setSeparator("@@");
        initializer.setDatabasePopulator(schemaPopulator);
        initializer.afterPropertiesSet();

        webTestClient.get()
                .uri("/api/v1/customer/{customerNumber}", created.getCustomerNumber())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("INACTIVE");

        assertThrows(RuntimeException.class, () -> databaseClient.sql("""
                INSERT INTO customers (
                    customer_number, customer_type, document_type, document_number,
                    profile_code, status, created_at, updated_at
                ) VALUES (10000999, 'PERSONAL', 'DNI', '87654321',
                    'STANDARD', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).fetch().rowsUpdated().block(Duration.ofSeconds(5)));
    }
}
