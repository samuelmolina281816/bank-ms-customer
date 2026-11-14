package com.nttdata.bank_ms_customer.domain.exception;

public class DuplicateCustomerDocumentException extends RuntimeException {
    public DuplicateCustomerDocumentException() {
        super("A customer with this document already exists");
    }
}
