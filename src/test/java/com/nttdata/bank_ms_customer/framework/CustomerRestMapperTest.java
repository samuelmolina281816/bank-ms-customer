package com.nttdata.bank_ms_customer.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nttdata.bank_ms_customer.application.dto.CustomerDto;
import com.nttdata.bank_ms_customer.application.dto.CustomerEligibilityDto;
import com.nttdata.bank_ms_customer.application.dto.UpdateCustomerCommand;
import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.CustomerStatus;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CreateCustomerRequest;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerEligibilityResponse;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerResponse;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.UpdateCustomerRequest;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.mapper.CustomerRestMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CustomerRestMapperTest {
    private final CustomerRestMapper mapper = new CustomerRestMapper();

    @Test
    void mapsCreateRequestWithOptionalProfile() {
        CreateCustomerRequest request = new CreateCustomerRequest()
                .customerType(com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerType.PERSONAL)
                .documentType(com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.DocumentType.DNI)
                .documentNumber("12345678")
                .firstName("Ana")
                .lastName("Lopez")
                .email("ana@example.com")
                .phone("5551234");

        var standardCommand = mapper.toCommand(request);
        request.profile(com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerProfile.VIP);
        var vipCommand = mapper.toCommand(request);

        assertNull(standardCommand.profile());
        assertEquals(CustomerType.PERSONAL, standardCommand.customerType());
        assertEquals(CustomerProfile.VIP, vipCommand.profile());
        assertEquals(DocumentType.DNI, vipCommand.documentType());
    }

    @Test
    void mapsUpdateRequestWithoutInventingUnsetFields() {
        UpdateCustomerRequest request = new UpdateCustomerRequest().email("new@example.com");

        UpdateCustomerCommand command = mapper.toCommand(request);

        assertEquals("new@example.com", command.email());
        assertNull(command.firstName());
        assertNull(command.phone());
    }

    @Test
    void mapsCustomerDtoToRestResponseInUtc() {
        Instant timestamp = Instant.parse("2026-01-01T12:30:00Z");
        CustomerDto dto = new CustomerDto(
                10_000_001L,
                CustomerType.PERSONAL,
                DocumentType.DNI,
                "12345678",
                "Ana",
                "Lopez",
                null,
                "ana@example.com",
                "5551234",
                null,
                CustomerProfile.VIP,
                CustomerStatus.ACTIVE,
                timestamp,
                timestamp);

        CustomerResponse response = mapper.toResponse(dto);

        assertEquals(10_000_001L, response.getCustomerNumber());
        assertEquals("VIP", response.getProfile().name());
        assertEquals(OffsetDateTime.ofInstant(timestamp, ZoneOffset.UTC), response.getCreatedAt());
        assertEquals("Ana", response.getFirstName());
    }

    @Test
    void mapsEligibilityDtoToMinimalRestResponse() {
        CustomerEligibilityDto dto = new CustomerEligibilityDto(
                10_000_001L, CustomerType.BUSINESS, CustomerProfile.PYME, CustomerStatus.ACTIVE, true);

        CustomerEligibilityResponse response = mapper.toEligibilityResponse(dto);

        assertEquals(10_000_001L, response.getCustomerNumber());
        assertEquals("BUSINESS", response.getCustomerType().name());
        assertEquals("PYME", response.getProfile().name());
        assertEquals(true, response.getEligible());
    }
}
