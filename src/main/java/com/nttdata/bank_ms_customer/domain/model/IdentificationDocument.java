package com.nttdata.bank_ms_customer.domain.model;

import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import java.util.Objects;

public record IdentificationDocument(DocumentType type, String number) {
    public IdentificationDocument {
        Objects.requireNonNull(type, "Document type is required");
        number = type.validateAndNormalize(number);
    }

    public boolean matches(IdentificationDocument other) {
        return other != null && type == other.type && number.equals(other.number);
    }
}
