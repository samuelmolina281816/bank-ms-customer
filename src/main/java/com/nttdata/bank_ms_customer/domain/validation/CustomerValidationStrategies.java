package com.nttdata.bank_ms_customer.domain.validation;

import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import java.util.Map;
import java.util.Objects;

public final class CustomerValidationStrategies {
    private static final Map<CustomerType, CustomerValidationStrategy> STRATEGIES = Map.of(
            CustomerType.PERSONAL, new PersonalCustomerValidationStrategy(),
            CustomerType.BUSINESS, new BusinessCustomerValidationStrategy());

    private CustomerValidationStrategies() {
    }

    public static CustomerValidationStrategy forType(CustomerType customerType) {
        return STRATEGIES.get(Objects.requireNonNull(customerType, "Customer type is required"));
    }
}
