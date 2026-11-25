package com.nttdata.bank_ms_customer.framework.adapter.input.rest.mapper;

import com.nttdata.bank_ms_customer.application.dto.CreateCustomerCommand;
import com.nttdata.bank_ms_customer.application.dto.CustomerDto;
import com.nttdata.bank_ms_customer.application.dto.CustomerEligibilityDto;
import com.nttdata.bank_ms_customer.application.dto.UpdateCustomerCommand;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CreateCustomerRequest;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerEligibilityResponse;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerResponse;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.UpdateCustomerRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class CustomerRestMapper {
    public CreateCustomerCommand toCommand(CreateCustomerRequest request) {
        return new CreateCustomerCommand(
                CustomerType.valueOf(request.getCustomerType().name()),
                DocumentType.valueOf(request.getDocumentType().name()),
                request.getDocumentNumber(),
                request.getFirstName(),
                request.getLastName(),
                request.getBusinessName(),
                request.getEmail(),
                request.getPhone(),
                request.getAddress(),
                request.getProfile() == null
                        ? null
                        : com.nttdata.bank_ms_customer.domain.enums.CustomerProfile.valueOf(request.getProfile().name()));
    }

    public UpdateCustomerCommand toCommand(UpdateCustomerRequest request) {
        return new UpdateCustomerCommand(
                request.getFirstName(),
                request.getLastName(),
                request.getBusinessName(),
                request.getEmail(),
                request.getPhone(),
                request.getAddress());
    }

    public CustomerResponse toResponse(CustomerDto customer) {
        return new CustomerResponse()
                .customerNumber(customer.customerNumber())
                .customerType(com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerType.valueOf(customer.customerType().name()))
                .documentType(com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.DocumentType.valueOf(customer.documentType().name()))
                .documentNumber(customer.documentNumber())
                .firstName(customer.firstName())
                .lastName(customer.lastName())
                .businessName(customer.businessName())
                .email(customer.email())
                .phone(customer.phone())
                .address(customer.address())
                .profile(com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerProfile.valueOf(customer.profile().name()))
                .status(com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerStatus.valueOf(customer.status().name()))
                .createdAt(OffsetDateTime.ofInstant(customer.createdAt(), ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.ofInstant(customer.updatedAt(), ZoneOffset.UTC));
    }

    public CustomerEligibilityResponse toEligibilityResponse(CustomerEligibilityDto eligibility) {
        return new CustomerEligibilityResponse()
                .customerNumber(eligibility.customerNumber())
                .customerType(com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerType.valueOf(eligibility.customerType().name()))
                .profile(com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerProfile.valueOf(eligibility.profile().name()))
                .status(com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerStatus.valueOf(eligibility.status().name()))
                .eligible(eligibility.eligible());
    }
}
