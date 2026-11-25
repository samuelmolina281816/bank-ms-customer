package com.nttdata.bank_ms_customer.domain.validation;

import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.exception.InvalidCustomerException;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;

public final class BusinessCustomerValidationStrategy implements CustomerValidationStrategy {
    @Override
    public void validateIdentity(
            IdentificationDocument document,
            String firstName,
            String lastName,
            String businessName) {
        if (document.type() != DocumentType.RUC
                || businessName == null
                || firstName != null
                || lastName != null) {
            throw new InvalidCustomerException("Business customers require RUC and businessName");
        }
        if (businessName.length() > 180) {
            throw new InvalidCustomerException("Business name cannot exceed 180 characters");
        }
    }

    @Override
    public void validateProfile(CustomerProfile profile) {
        if (profile != CustomerProfile.STANDARD && profile != CustomerProfile.PYME) {
            throw new InvalidCustomerException("Customer profile is not compatible with BUSINESS type");
        }
    }
}
