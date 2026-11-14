package com.nttdata.bank_ms_customer.application.dto;

import com.nttdata.bank_ms_customer.domain.model.Customer;
import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.CustomerStatus;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import java.time.Instant;

public record CustomerDto(
        Long customerNumber,
        CustomerType customerType,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String businessName,
        String email,
        String phone,
        String address,
        CustomerProfile profile,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static CustomerDto from(Customer customer) {
        return new CustomerDto(
                customer.getCustomerNumber(),
                customer.getCustomerType(),
                customer.getIdentificationDocument().type(),
                customer.getIdentificationDocument().number(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getBusinessName(),
                customer.getContactInformation().email(),
                customer.getContactInformation().phone(),
                customer.getContactInformation().address(),
                customer.getProfile(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
