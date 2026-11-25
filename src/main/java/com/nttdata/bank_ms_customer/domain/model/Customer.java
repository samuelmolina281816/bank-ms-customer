package com.nttdata.bank_ms_customer.domain.model;

import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.CustomerStatus;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.exception.InvalidCustomerException;
import com.nttdata.bank_ms_customer.domain.validation.CustomerValidationStrategies;
import java.time.Instant;
import java.util.Objects;

public final class Customer {
    private Long id;
    private final Long customerNumber;
    private final CustomerType customerType;
    private final IdentificationDocument identificationDocument;
    private ContactInformation contactInformation;
    private final CustomerProfile profile;
    private CustomerStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private String firstName;
    private String lastName;
    private String businessName;

    public Customer(
            Long id,
            Long customerNumber,
            CustomerType customerType,
            IdentificationDocument identificationDocument,
            ContactInformation contactInformation,
            CustomerProfile profile,
            CustomerStatus status,
            Instant createdAt,
            Instant updatedAt,
            String firstName,
            String lastName,
            String businessName) {
        this.id = id;
        this.customerNumber = Objects.requireNonNull(customerNumber, "Customer number is required");
        this.customerType = Objects.requireNonNull(customerType, "Customer type is required");
        this.identificationDocument = Objects.requireNonNull(identificationDocument, "Document is required");
        this.contactInformation = Objects.requireNonNull(contactInformation, "Contact information is required");
        this.profile = Objects.requireNonNull(profile, "Profile is required");
        this.status = Objects.requireNonNull(status, "Status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update time is required");
        this.firstName = normalizeOptional(firstName);
        this.lastName = normalizeOptional(lastName);
        this.businessName = normalizeOptional(businessName);
        validate();
    }

    public static Customer create(
            Long customerNumber,
            CustomerType customerType,
            IdentificationDocument document,
            ContactInformation contactInformation,
            CustomerProfile profile,
            String firstName,
            String lastName,
            String businessName,
            Instant now) {
        return new Customer(null, customerNumber, customerType, document, contactInformation,
                profile, CustomerStatus.ACTIVE, now, now,
                firstName, lastName, businessName);
    }

    public static Customer create(
            Long customerNumber,
            CustomerType customerType,
            IdentificationDocument document,
            ContactInformation contactInformation,
            String firstName,
            String lastName,
            String businessName,
            Instant now) {
        return create(customerNumber, customerType, document, contactInformation,
                CustomerProfile.STANDARD, firstName, lastName, businessName, now);
    }

    public static void validateIdentity(
            CustomerType customerType,
            IdentificationDocument document,
            String firstName,
            String lastName,
            String businessName) {
        Objects.requireNonNull(customerType, "Customer type is required");
        Objects.requireNonNull(document, "Document is required");
        String normalizedFirstName = normalizeOptional(firstName);
        String normalizedLastName = normalizeOptional(lastName);
        String normalizedBusinessName = normalizeOptional(businessName);
        CustomerValidationStrategies.forType(customerType).validateIdentity(
                document, normalizedFirstName, normalizedLastName, normalizedBusinessName);
    }

    public void update(
            String newFirstName,
            String newLastName,
            String newBusinessName,
            String email,
            String phone,
            String address,
            Instant now) {
        if (newFirstName != null) {
            firstName = normalizeOptional(newFirstName);
        }
        if (newLastName != null) {
            lastName = normalizeOptional(newLastName);
        }
        if (newBusinessName != null) {
            businessName = normalizeOptional(newBusinessName);
        }
        contactInformation = contactInformation.merge(email, phone, address);
        updatedAt = Objects.requireNonNull(now, "Update time is required");
        validate();
    }

    public void deactivate(Instant now) {
        if (status != CustomerStatus.INACTIVE) {
            status = CustomerStatus.INACTIVE;
            updatedAt = Objects.requireNonNull(now, "Update time is required");
        }
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public void assignPersistenceId(Long id) {
        this.id = id;
    }

    private void validate() {
        if (customerNumber < 10_000_000L || customerNumber > 99_999_999L) {
            throw new InvalidCustomerException("Customer number must contain 8 digits");
        }
        validateIdentity(customerType, identificationDocument, firstName, lastName, businessName);
        validateProfileCompatibility(customerType, profile);
    }

    public static void validateProfileCompatibility(CustomerType customerType, CustomerProfile profile) {
        Objects.requireNonNull(profile, "Profile is required");
        CustomerValidationStrategies.forType(customerType).validateProfile(profile);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public Long getId() { return id; }
    public Long getCustomerNumber() { return customerNumber; }
    public CustomerType getCustomerType() { return customerType; }
    public IdentificationDocument getIdentificationDocument() { return identificationDocument; }
    public ContactInformation getContactInformation() { return contactInformation; }
    public CustomerProfile getProfile() { return profile; }
    public CustomerStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getBusinessName() { return businessName; }
}
