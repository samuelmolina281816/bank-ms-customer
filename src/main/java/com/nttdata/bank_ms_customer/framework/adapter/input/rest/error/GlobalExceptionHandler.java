package com.nttdata.bank_ms_customer.framework.adapter.input.rest.error;

import com.nttdata.bank_ms_customer.domain.exception.CustomerNotFoundException;
import com.nttdata.bank_ms_customer.domain.exception.DuplicateCustomerDocumentException;
import com.nttdata.bank_ms_customer.domain.exception.InvalidCustomerException;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.ApiError;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.ApiErrorDetail;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(CustomerNotFoundException exception, ServerWebExchange exchange) {
        return response(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", exception.getMessage(), exchange);
    }

    @ExceptionHandler(DuplicateCustomerDocumentException.class)
    public ResponseEntity<ApiError> handleDuplicate(
            DuplicateCustomerDocumentException exception,
            ServerWebExchange exchange) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_CUSTOMER_DOCUMENT", exception.getMessage(), exchange);
    }

    @ExceptionHandler(InvalidCustomerException.class)
    public ResponseEntity<ApiError> handleInvalid(InvalidCustomerException exception, ServerWebExchange exchange) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "CUSTOMER_VALIDATION_ERROR", exception.getMessage(), exchange);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(
            InvalidRequestException exception,
            ServerWebExchange exchange) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleRequestValidation(
            WebExchangeBindException exception,
            ServerWebExchange exchange) {
        ApiError body = baseError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", exchange);
        List<ApiErrorDetail> details = exception.getFieldErrors().stream()
                .map(this::toErrorDetail)
                .toList();
        body.details(details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, ServerWebExchange exchange) {
        String correlationId = CorrelationIdWebFilter.getOrCreate(exchange);
        LOGGER.error("Unhandled request failure; correlationId={}, exceptionType={}",
                correlationId, exception.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", exchange);
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String code,
            String message,
            ServerWebExchange exchange) {
        return ResponseEntity.status(status).body(baseError(status, code, message, exchange));
    }

    private ApiError baseError(
            HttpStatus status,
            String code,
            String message,
            ServerWebExchange exchange) {
        String correlationId = CorrelationIdWebFilter.getOrCreate(exchange);
        return new ApiError()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(status.value())
                .code(code)
                .message(message)
                .path(exchange.getRequest().getPath().value())
                .correlationId(correlationId);
    }

    private ApiErrorDetail toErrorDetail(FieldError error) {
        return new ApiErrorDetail()
                .field(error.getField())
                .reason(error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage());
    }
}
