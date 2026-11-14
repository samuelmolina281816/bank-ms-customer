package com.nttdata.bank_ms_customer.domain.exception;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException() {
        super("Customer was not found");
    }
}
