package com.nttdata.bank_ms_customer.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.CustomerStatus;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.exception.InvalidCustomerException;
import com.nttdata.bank_ms_customer.domain.model.ContactInformation;
import com.nttdata.bank_ms_customer.domain.model.Customer;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CustomerDomainBoundaryTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void acceptsCustomerNumberBounds() {
        Customer minimum = personalCustomer(10_000_000L);
        Customer maximum = personalCustomer(99_999_999L);

        assertEquals(10_000_000L, minimum.getCustomerNumber());
        assertEquals(99_999_999L, maximum.getCustomerNumber());
    }

    @Test
    void rejectsCustomerNumbersOutsideEightDigits() {
        assertThrows(InvalidCustomerException.class, () -> personalCustomer(9_999_999L));
        assertThrows(InvalidCustomerException.class, () -> personalCustomer(100_000_000L));
    }

    @Test
    void mergesOnlyProvidedContactFields() {
        ContactInformation original = new ContactInformation("ana@example.com", "5551234", "Street");

        ContactInformation updated = original.merge("new@example.com", null, "New street");

        assertEquals("new@example.com", updated.email());
        assertEquals("5551234", updated.phone());
        assertEquals("New street", updated.address());
        assertNotSame(original, updated);
    }

    @Test
    void rejectsInvalidContactInformation() {
        assertThrows(InvalidCustomerException.class,
                () -> new ContactInformation("invalid", "5551234", null));
        assertThrows(InvalidCustomerException.class,
                () -> new ContactInformation("ana@example.com", " ", null));
        assertThrows(InvalidCustomerException.class,
                () -> new ContactInformation("ana@example.com", "5551234", "x".repeat(251)));
    }

    @Test
    void matchesDocumentsOnlyWhenTypeAndNormalizedNumberMatch() {
        IdentificationDocument dni = new IdentificationDocument(DocumentType.DNI, "12345678");
        IdentificationDocument sameDni = new IdentificationDocument(DocumentType.DNI, "12345678");
        IdentificationDocument otherDni = new IdentificationDocument(DocumentType.DNI, "87654321");
        IdentificationDocument passport = new IdentificationDocument(DocumentType.PASSPORT, "12345678");

        assertTrue(dni.matches(sameDni));
        assertFalse(dni.matches(otherDni));
        assertFalse(dni.matches(passport));
        assertFalse(dni.matches(null));
    }

    @Test
    void rejectsInvalidDocumentFormats() {
        assertThrows(InvalidCustomerException.class, () -> new IdentificationDocument(DocumentType.DNI, "123"));
        assertThrows(InvalidCustomerException.class,
                () -> new IdentificationDocument(DocumentType.RUC, "2012345678"));
        assertThrows(InvalidCustomerException.class, () -> new IdentificationDocument(DocumentType.CE, "AB12"));
        assertThrows(InvalidCustomerException.class,
                () -> new IdentificationDocument(DocumentType.PASSPORT, "ABCDEFGHIJKLM"));
    }

    @Test
    void updatesCustomerFieldsAndDeactivationIsIdempotent() {
        Customer customer = personalCustomer(10_000_001L);

        customer.update(" Ana ", null, null, "new@example.com", null, "New street",
                Instant.parse("2026-01-02T00:00:00Z"));
        customer.deactivate(Instant.parse("2026-01-03T00:00:00Z"));
        customer.deactivate(Instant.parse("2026-01-04T00:00:00Z"));

        assertEquals("Ana", customer.getFirstName());
        assertEquals("Lopez", customer.getLastName());
        assertEquals("new@example.com", customer.getContactInformation().email());
        assertEquals("New street", customer.getContactInformation().address());
        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
        assertEquals(Instant.parse("2026-01-03T00:00:00Z"), customer.getUpdatedAt());
    }

    @Test
    void rejectsIdentityFieldsThatDoNotMatchPersonalOrBusinessType() {
        IdentificationDocument dni = new IdentificationDocument(DocumentType.DNI, "12345678");
        IdentificationDocument ruc = new IdentificationDocument(DocumentType.RUC, "20123456789");

        assertThrows(InvalidCustomerException.class,
                () -> Customer.validateIdentity(CustomerType.PERSONAL, dni, null, "Lopez", null));
        assertThrows(InvalidCustomerException.class,
                () -> Customer.validateIdentity(CustomerType.PERSONAL, dni, "Ana", "Lopez", "Company"));
        assertThrows(InvalidCustomerException.class,
                () -> Customer.validateIdentity(CustomerType.BUSINESS, ruc, "Ana", null, "Company"));
        assertThrows(InvalidCustomerException.class,
                () -> Customer.validateIdentity(CustomerType.BUSINESS, ruc, null, null, null));
    }

    private Customer personalCustomer(Long customerNumber) {
        return Customer.create(
                customerNumber,
                CustomerType.PERSONAL,
                new IdentificationDocument(DocumentType.DNI, "12345678"),
                new ContactInformation("ana@example.com", "5551234", "Street"),
                CustomerProfile.STANDARD,
                "Ana",
                "Lopez",
                null,
                NOW);
    }
}
