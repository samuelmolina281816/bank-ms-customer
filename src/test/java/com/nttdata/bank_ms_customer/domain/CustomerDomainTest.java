package com.nttdata.bank_ms_customer.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.exception.InvalidCustomerException;
import com.nttdata.bank_ms_customer.domain.model.ContactInformation;
import com.nttdata.bank_ms_customer.domain.model.Customer;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CustomerDomainTest {
    @Test
    void createsPersonalCustomerWithDni() {
        Customer customer = Customer.create(
                10_000_001L,
                CustomerType.PERSONAL,
                new IdentificationDocument(DocumentType.DNI, "00123456"),
                new ContactInformation("ana@example.com", "5551234", null),
                "Ana",
                "Lopez",
                null,
                Instant.parse("2026-01-01T00:00:00Z"));

        assertEquals("00123456", customer.getIdentificationDocument().number());
    }

    @Test
    void acceptsAlphanumericCeAndPassportWithinConfirmedLength() {
        assertEquals("A1234", new IdentificationDocument(DocumentType.CE, "a1234").number());
        assertEquals("P12AB", new IdentificationDocument(DocumentType.PASSPORT, "p12ab").number());
    }

    @Test
    void rejectsPersonalCustomerWithBusinessDocument() {
        IdentificationDocument ruc = new IdentificationDocument(DocumentType.RUC, "20123456789");

        assertThrows(InvalidCustomerException.class,
                () -> Customer.validateIdentity(CustomerType.PERSONAL, ruc, "Ana", "Lopez", null));
    }

    @Test
    void acceptsProfilesForTheirCustomerTypes() {
        assertEquals(CustomerProfile.VIP, createCustomer(CustomerType.PERSONAL, CustomerProfile.VIP)
                .getProfile());
        assertEquals(CustomerProfile.PYME, new Customer(
                null,
                10_000_002L,
                CustomerType.BUSINESS,
                new IdentificationDocument(DocumentType.RUC, "20123456789"),
                new ContactInformation("empresa@example.com", "5551234", null),
                CustomerProfile.PYME,
                com.nttdata.bank_ms_customer.domain.enums.CustomerStatus.ACTIVE,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                null,
                "Empresa S.A.").getProfile());
    }

    @Test
    void rejectsProfilesIncompatibleWithCustomerTypes() {
        assertThrows(InvalidCustomerException.class,
                () -> Customer.validateProfileCompatibility(CustomerType.PERSONAL, CustomerProfile.PYME));
        assertThrows(InvalidCustomerException.class,
                () -> Customer.validateProfileCompatibility(CustomerType.BUSINESS, CustomerProfile.VIP));
    }

    @Test
    void deactivationChangesEligibilityState() {
        Customer customer = Customer.create(
                10_000_001L,
                CustomerType.PERSONAL,
                new IdentificationDocument(DocumentType.DNI, "12345678"),
                new ContactInformation("ana@example.com", "5551234", null),
                "Ana",
                "Lopez",
                null,
                Instant.parse("2026-01-01T00:00:00Z"));

        customer.deactivate(Instant.parse("2026-01-02T00:00:00Z"));

        assertEquals("INACTIVE", customer.getStatus().name());
        assertEquals(Instant.parse("2026-01-02T00:00:00Z"), customer.getUpdatedAt());
    }

    private Customer createCustomer(CustomerType type, CustomerProfile profile) {
        return Customer.create(
                10_000_001L,
                type,
                new IdentificationDocument(DocumentType.DNI, "00123456"),
                new ContactInformation("ana@example.com", "5551234", null),
                profile,
                "Ana",
                "Lopez",
                null,
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
