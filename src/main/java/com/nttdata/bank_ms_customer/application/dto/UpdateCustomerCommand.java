package com.nttdata.bank_ms_customer.application.dto;

public record UpdateCustomerCommand(
        String firstName,
        String lastName,
        String businessName,
        String email,
        String phone,
        String address) {
}
