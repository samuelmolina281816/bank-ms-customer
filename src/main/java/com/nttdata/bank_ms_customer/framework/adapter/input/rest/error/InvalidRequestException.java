package com.nttdata.bank_ms_customer.framework.adapter.input.rest.error;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
