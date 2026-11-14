package com.nttdata.bank_ms_customer.domain.enums;

import com.nttdata.bank_ms_customer.domain.exception.InvalidCustomerException;
import java.util.regex.Pattern;

public enum DocumentType {
    DNI(Pattern.compile("[0-9]{8}")),
    RUC(Pattern.compile("[0-9]{11}")),
    CE(Pattern.compile("[A-Za-z0-9]{5,12}")),
    PASSPORT(Pattern.compile("[A-Za-z0-9]{5,12}"));

    private final Pattern pattern;

    DocumentType(Pattern pattern) {
        this.pattern = pattern;
    }

    public String validateAndNormalize(String value) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new InvalidCustomerException("Invalid " + name() + " document format");
        }
        return value.toUpperCase();
    }
}
