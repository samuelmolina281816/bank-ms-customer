package com.nttdata.bank_ms_customer.domain.validation;

import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.exception.InvalidCustomerException;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;

public final class PersonalCustomerValidationStrategy implements CustomerValidationStrategy {
    @Override
    public void validateIdentity(
            IdentificationDocument document,
            String firstName,
            String lastName,
            String businessName) {
        DocumentType documentType = document.type();
        if ((documentType != DocumentType.DNI
                && documentType != DocumentType.CE
                && documentType != DocumentType.PASSPORT)
                || firstName == null
                || lastName == null
                || businessName != null) {
            throw new InvalidCustomerException(
                    "Personal customers require firstName, lastName and a personal document");
        }
        if (firstName.length() > 100 || lastName.length() > 100) {
            throw new InvalidCustomerException("Personal customer names cannot exceed 100 characters");
        }
    }

    @Override
    public void validateProfile(CustomerProfile profile) {
        if (profile != CustomerProfile.STANDARD && profile != CustomerProfile.VIP) {
            throw new InvalidCustomerException("Customer profile is not compatible with PERSONAL type");
        }
    }
}
