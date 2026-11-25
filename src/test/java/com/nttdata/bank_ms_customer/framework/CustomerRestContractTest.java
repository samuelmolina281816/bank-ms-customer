package com.nttdata.bank_ms_customer.framework;

import static org.springframework.test.web.reactive.server.WebTestClient.bindToServer;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.cloud.config.enabled=false", "spring.sql.init.mode=never"})
class CustomerRestContractTest {
    @LocalServerPort
    private int port;

    @Test
    void generatedApiInterfaceExposesImplementedCustomerRoute() {
        WebTestClient client = bindToServer().baseUrl("http://localhost:" + port).build();

        client.post()
                .uri("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "customerType": "PERSONAL",
                          "documentType": "RUC",
                          "documentNumber": "20123456789",
                          "firstName": "Ana",
                          "lastName": "Lopez",
                          "email": "ana@example.com",
                          "phone": "5551234"
                        }
                        """)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectHeader().exists("X-Correlation-Id")
                .expectBody()
                .jsonPath("$.code").isEqualTo("CUSTOMER_VALIDATION_ERROR");
    }

    @Test
    void springDocAndSwaggerServeTheContractAndImplementedOperations() {
        WebTestClient client = bindToServer().baseUrl("http://localhost:" + port).build();

        String springDoc = client.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertTrue(springDoc != null && springDoc.contains("/api/v1/customer"));
        assertTrue(springDoc.contains("/api/v1/customer/{customerNumber}/eligibility"));
        assertTrue(springDoc.contains("createCustomer"));

        String sourceContract = client.get()
                .uri("/openapi.yaml")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertTrue(sourceContract != null && sourceContract.contains("Bank Customer API"));
        assertTrue(sourceContract.contains("getCustomerEligibility"));
    }

    @Test
    void invalidRequestUsesApiErrorContractWithoutEchoingSubmittedFields() {
        WebTestClient client = bindToServer().baseUrl("http://localhost:" + port).build();

        client.post()
                .uri("/api/v1/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"not-an-email"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.correlationId").exists()
                .jsonPath("$.details[0].rejectedValue").doesNotExist();
    }

    @Test
    void updateRejectsAnEmptyRepresentation() {
        WebTestClient client = bindToServer().baseUrl("http://localhost:" + port).build();

        client.put()
                .uri("/api/v1/customer/{customerNumber}", 10_000_001L)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }
}
