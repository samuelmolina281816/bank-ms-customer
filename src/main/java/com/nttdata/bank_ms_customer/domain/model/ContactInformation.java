package com.nttdata.bank_ms_customer.domain.model;

import com.nttdata.bank_ms_customer.domain.exception.InvalidCustomerException;
import java.util.Objects;
import java.util.regex.Pattern;

public record ContactInformation(String email, String phone, String address) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public ContactInformation {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches() || email.length() > 150) {
            throw new InvalidCustomerException("A valid email is required");
        }
        if (phone == null || phone.isBlank() || phone.length() > 30) {
            throw new InvalidCustomerException("A valid phone is required");
        }
        if (address != null && address.length() > 250) {
            throw new InvalidCustomerException("Address cannot exceed 250 characters");
        }
    }

    public ContactInformation merge(String newEmail, String newPhone, String newAddress) {
        return new ContactInformation(
                newEmail == null ? email : newEmail,
                newPhone == null ? phone : newPhone,
                newAddress == null ? address : newAddress);
    }
}
