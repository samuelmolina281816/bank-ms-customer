package com.nttdata.bank_ms_customer.domain.validation;

import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;

public interface CustomerValidationStrategy {
    void validateIdentity(
            IdentificationDocument document,
            String firstName,
            String lastName,
            String businessName);

    void validateProfile(CustomerProfile profile);
}
