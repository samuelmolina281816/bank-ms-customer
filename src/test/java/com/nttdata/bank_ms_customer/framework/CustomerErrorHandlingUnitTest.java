package com.nttdata.bank_ms_customer.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nttdata.bank_ms_customer.domain.exception.CustomerNotFoundException;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.error.CorrelationIdWebFilter;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class CustomerErrorHandlingUnitTest {
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private final CorrelationIdWebFilter correlationIdFilter = new CorrelationIdWebFilter();

    @Test
    void mapsCustomerNotFoundToMinimalNotFoundError() {
        MockServerWebExchange exchange = exchange();

        var response = exceptionHandler.handleNotFound(new CustomerNotFoundException(), exchange);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("CUSTOMER_NOT_FOUND", response.getBody().getCode());
        assertNotNull(response.getBody().getCorrelationId());
    }

    @Test
    void mapsUnexpectedExceptionWithoutExposingInternalMessage() {
        MockServerWebExchange exchange = exchange();

        var response = exceptionHandler.handleUnexpected(new IllegalStateException("secret detail"), exchange);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().getCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertTrue(response.getBody().getCorrelationId() != null);
    }

    @Test
    void acceptsSafeIncomingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/customers").header("X-Correlation-Id", "request-123").build());

        correlationIdFilter.filter(exchange, ignored -> Mono.empty()).block();

        assertEquals("request-123", exchange.getResponse().getHeaders().getFirst("X-Correlation-Id"));
        assertEquals("request-123",
                exchange.getAttribute(CorrelationIdWebFilter.ATTRIBUTE));
    }

    @Test
    void replacesUnsafeIncomingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/customers").header("X-Correlation-Id", "invalid value").build());

        correlationIdFilter.filter(exchange, ignored -> Mono.empty()).block();

        String correlationId = exchange.getResponse().getHeaders().getFirst("X-Correlation-Id");
        assertNotNull(correlationId);
        assertTrue(correlationId.matches("[0-9a-f-]{36}"));
        assertEquals(correlationId, exchange.getAttribute(CorrelationIdWebFilter.ATTRIBUTE));
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/customers").build());
    }
}
