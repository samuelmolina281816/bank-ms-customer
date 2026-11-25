package com.nttdata.bank_ms_customer.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.CustomerStatus;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.model.ContactInformation;
import com.nttdata.bank_ms_customer.domain.model.Customer;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.BusinessCustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.CustomerContactEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.CustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.PersonalCustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.mapper.CustomerPersistenceMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CustomerPersistenceMapperTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final CustomerPersistenceMapper mapper = new CustomerPersistenceMapper();

    @Test
    void mapsPersonalCustomerToPersistenceEntities() {
        Customer customer = personalCustomer();
        customer.assignPersistenceId(15L);

        CustomerEntity root = mapper.toCustomerEntity(customer);
        PersonalCustomerEntity personal = mapper.toPersonalEntity(customer, true);
        CustomerContactEntity contact = mapper.toContactEntity(customer, true);

        assertEquals("PERSONAL", root.getCustomerType());
        assertEquals("VIP", root.getProfileCode());
        assertEquals(15L, personal.getCustomerId());
        assertEquals("Ana", personal.getFirstName());
        assertEquals("ana@example.com", contact.getEmail());
    }

    @Test
    void mapsBusinessEntitiesBackToDomain() {
        CustomerEntity root = customerEntity("BUSINESS", "PYME", "RUC", "20123456789");
        BusinessCustomerEntity business = new BusinessCustomerEntity();
        business.setCustomerId(15L);
        business.setBusinessName("Empresa");
        CustomerContactEntity contact = contactEntity();

        Customer customer = mapper.toDomain(root, null, business, contact);

        assertEquals(CustomerType.BUSINESS, customer.getCustomerType());
        assertEquals(CustomerProfile.PYME, customer.getProfile());
        assertEquals("Empresa", customer.getBusinessName());
        assertNull(customer.getFirstName());
    }

    @Test
    void rejectsAggregateMissingContactOrMatchingSubtype() {
        CustomerEntity personalRoot = customerEntity("PERSONAL", "STANDARD", "DNI", "12345678");
        PersonalCustomerEntity personal = new PersonalCustomerEntity();
        personal.setCustomerId(15L);
        personal.setFirstName("Ana");
        personal.setLastName("Lopez");

        assertThrows(IllegalStateException.class, () -> mapper.toDomain(personalRoot, personal, null, null));
        assertThrows(IllegalStateException.class, () -> mapper.toDomain(personalRoot, null, null, contactEntity()));
        assertThrows(IllegalStateException.class,
                () -> mapper.toDomain(personalRoot, personal, new BusinessCustomerEntity(), contactEntity()));
    }

    private Customer personalCustomer() {
        return Customer.create(
                10_000_001L,
                CustomerType.PERSONAL,
                new IdentificationDocument(DocumentType.DNI, "12345678"),
                new ContactInformation("ana@example.com", "5551234", null),
                CustomerProfile.VIP,
                "Ana",
                "Lopez",
                null,
                NOW);
    }

    private CustomerEntity customerEntity(String type, String profile, String documentType, String documentNumber) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(15L);
        entity.setCustomerNumber(10_000_001L);
        entity.setCustomerType(type);
        entity.setDocumentType(documentType);
        entity.setDocumentNumber(documentNumber);
        entity.setProfileCode(profile);
        entity.setStatus(CustomerStatus.ACTIVE.name());
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        return entity;
    }

    private CustomerContactEntity contactEntity() {
        CustomerContactEntity contact = new CustomerContactEntity();
        contact.setCustomerId(15L);
        contact.setEmail("company@example.com");
        contact.setPhone("5551234");
        return contact;
    }
}
