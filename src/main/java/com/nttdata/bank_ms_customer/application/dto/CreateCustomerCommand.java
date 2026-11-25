package com.nttdata.bank_ms_customer.application.dto;

import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;

public record CreateCustomerCommand(
        CustomerType customerType,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String businessName,
        String email,
        String phone,
        String address,
        CustomerProfile profile) {
}
