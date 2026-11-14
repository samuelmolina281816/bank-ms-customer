package com.nttdata.bank_ms_customer.framework.adapter.output.repository.mapper;

import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.CustomerStatus;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.model.ContactInformation;
import com.nttdata.bank_ms_customer.domain.model.Customer;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.BusinessCustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.CustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.CustomerContactEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.PersonalCustomerEntity;
import io.r2dbc.spi.Row;
import org.springframework.stereotype.Component;

@Component
public class CustomerPersistenceMapper {
    public CustomerEntity toCustomerEntity(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customer.getId());
        entity.setCustomerNumber(customer.getCustomerNumber());
        entity.setCustomerType(customer.getCustomerType().name());
        entity.setDocumentType(customer.getIdentificationDocument().type().name());
        entity.setDocumentNumber(customer.getIdentificationDocument().number());
        entity.setProfileCode(customer.getProfile().name());
        entity.setStatus(customer.getStatus().name());
        entity.setCreatedAt(customer.getCreatedAt());
        entity.setUpdatedAt(customer.getUpdatedAt());
        return entity;
    }

    public PersonalCustomerEntity toPersonalEntity(Customer customer, boolean newCustomer) {
        PersonalCustomerEntity entity = new PersonalCustomerEntity();
        entity.setCustomerId(customer.getId());
        entity.setFirstName(customer.getFirstName());
        entity.setLastName(customer.getLastName());
        entity.setNewRecord(newCustomer);
        return entity;
    }

    public BusinessCustomerEntity toBusinessEntity(Customer customer, boolean newCustomer) {
        BusinessCustomerEntity entity = new BusinessCustomerEntity();
        entity.setCustomerId(customer.getId());
        entity.setBusinessName(customer.getBusinessName());
        entity.setNewRecord(newCustomer);
        return entity;
    }

    public CustomerContactEntity toContactEntity(Customer customer, boolean newCustomer) {
        CustomerContactEntity entity = new CustomerContactEntity();
        entity.setCustomerId(customer.getId());
        entity.setEmail(customer.getContactInformation().email());
        entity.setPhone(customer.getContactInformation().phone());
        entity.setAddress(customer.getContactInformation().address());
        entity.setNewRecord(newCustomer);
        return entity;
    }

    public Customer toDomain(
            CustomerEntity entity,
            PersonalCustomerEntity personal,
            BusinessCustomerEntity business,
            CustomerContactEntity contact) {
        if (contact == null
                || (entity.getCustomerType().equals(CustomerType.PERSONAL.name())
                && (personal == null || business != null))
                || (entity.getCustomerType().equals(CustomerType.BUSINESS.name())
                && (business == null || personal != null))) {
            throw new IllegalStateException("Persisted customer aggregate is incomplete or inconsistent");
        }
        return new Customer(
                entity.getId(),
                entity.getCustomerNumber(),
                CustomerType.valueOf(entity.getCustomerType()),
                new IdentificationDocument(DocumentType.valueOf(entity.getDocumentType()), entity.getDocumentNumber()),
                new ContactInformation(contact.getEmail(), contact.getPhone(), contact.getAddress()),
                CustomerProfile.valueOf(entity.getProfileCode()),
                CustomerStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                personal == null ? null : personal.getFirstName(),
                personal == null ? null : personal.getLastName(),
                business == null ? null : business.getBusinessName());
    }

    public Customer toDomain(Row row) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(row.get("id", Long.class));
        customer.setCustomerNumber(row.get("customer_number", Long.class));
        customer.setCustomerType(row.get("customer_type", String.class));
        customer.setDocumentType(row.get("document_type", String.class));
        customer.setDocumentNumber(row.get("document_number", String.class));
        customer.setProfileCode(row.get("profile_code", String.class));
        customer.setStatus(row.get("status", String.class));
        customer.setCreatedAt(row.get("created_at", java.time.Instant.class));
        customer.setUpdatedAt(row.get("updated_at", java.time.Instant.class));

        Long personalId = row.get("personal_customer_id", Long.class);
        PersonalCustomerEntity personal = null;
        if (personalId != null) {
            personal = new PersonalCustomerEntity();
            personal.setCustomerId(personalId);
            personal.setFirstName(row.get("first_name", String.class));
            personal.setLastName(row.get("last_name", String.class));
        }

        Long businessId = row.get("business_customer_id", Long.class);
        BusinessCustomerEntity business = null;
        if (businessId != null) {
            business = new BusinessCustomerEntity();
            business.setCustomerId(businessId);
            business.setBusinessName(row.get("business_name", String.class));
        }

        Long contactId = row.get("contact_customer_id", Long.class);
        CustomerContactEntity contact = null;
        if (contactId != null) {
            contact = new CustomerContactEntity();
            contact.setCustomerId(contactId);
            contact.setEmail(row.get("email", String.class));
            contact.setPhone(row.get("phone", String.class));
            contact.setAddress(row.get("address", String.class));
        }
        return toDomain(customer, personal, business, contact);
    }
}
