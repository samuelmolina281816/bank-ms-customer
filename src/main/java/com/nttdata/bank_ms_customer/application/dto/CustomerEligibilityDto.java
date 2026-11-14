package com.nttdata.bank_ms_customer.application.dto;

import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.CustomerStatus;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;

public record CustomerEligibilityDto(
        Long customerNumber,
        CustomerType customerType,
        CustomerProfile profile,
        CustomerStatus status,
        boolean eligible) {
}
