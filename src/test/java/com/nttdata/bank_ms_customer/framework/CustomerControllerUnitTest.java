package com.nttdata.bank_ms_customer.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nttdata.bank_ms_customer.application.dto.CreateCustomerCommand;
import com.nttdata.bank_ms_customer.application.dto.CustomerDto;
import com.nttdata.bank_ms_customer.application.dto.CustomerEligibilityDto;
import com.nttdata.bank_ms_customer.application.dto.UpdateCustomerCommand;
import com.nttdata.bank_ms_customer.application.port.input.CustomerEligibilityInputPort;
import com.nttdata.bank_ms_customer.application.port.input.CustomerInputPort;
import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.CustomerStatus;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.exception.CustomerNotFoundException;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.controller.CustomerController;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.controller.CustomerEligibilityController;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.mapper.CustomerRestMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerControllerUnitTest {
    @Mock
    private CustomerInputPort customerInput;
    @Mock
    private CustomerEligibilityInputPort eligibilityInput;

    private final CustomerRestMapper mapper = new CustomerRestMapper();

    @Test
    void createsCustomerAndReturnsCreatedResponse() {
        when(customerInput.create(any(CreateCustomerCommand.class))).thenReturn(Single.just(customerDto()));

        var response = new CustomerController(customerInput, mapper)
                .createCustomer(
                        reactor.core.publisher.Mono.just(createRequest()),
                        null)
                .block();

        assertEquals(201, response.getStatusCode().value());
        assertEquals(10_000_001L, response.getBody().getCustomerNumber());
    }

    @Test
    void updatesCustomerAndReturnsUpdatedResponse() {
        when(customerInput.update(eq(10_000_001L), any(UpdateCustomerCommand.class)))
                .thenReturn(Single.just(customerDto()));

        var request = new com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model
                .UpdateCustomerRequest().email("updated@example.com");
        var response = new CustomerController(customerInput, mapper)
                .updateCustomer(10_000_001L, reactor.core.publisher.Mono.just(request), null)
                .block();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ana@example.com", response.getBody().getEmail());
    }

    @Test
    void deactivatesCustomerAndReturnsNoContent() {
        when(customerInput.deactivate(10_000_001L)).thenReturn(Completable.complete());

        var response = new CustomerController(customerInput, mapper)
                .deactivateCustomer(10_000_001L, null)
                .block();

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void listsCustomersAsRestResponses() {
        when(customerInput.findAll()).thenReturn(Flowable.just(customerDto()));

        var response = new CustomerController(customerInput, mapper).findAllCustomers(null).block();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(10_000_001L, response.getBody().collectList().block().get(0).getCustomerNumber());
    }

    @Test
    void returnsCustomerByNumber() {
        CustomerDto customer = customerDto();
        when(customerInput.findByCustomerNumber(10_000_001L))
                .thenReturn(Maybe.just(customer));

        var response = new CustomerController(customerInput, mapper)
                .findCustomerByNumber(10_000_001L, null)
                .block();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(10_000_001L, response.getBody().getCustomerNumber());
    }

    @Test
    void reportsMissingCustomerByNumber() {
        when(customerInput.findByCustomerNumber(10_000_099L))
                .thenReturn(Maybe.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> new CustomerController(customerInput, mapper)
                        .findCustomerByNumber(10_000_099L, null)
                        .block());
    }

    @Test
    void returnsEligibilityByPublicNumber() {
        CustomerEligibilityDto eligibility = new CustomerEligibilityDto(
                10_000_001L, CustomerType.PERSONAL, CustomerProfile.VIP, CustomerStatus.ACTIVE, true);
        when(eligibilityInput.getEligibilityByCustomerNumber(10_000_001L))
                .thenReturn(Single.just(eligibility));

        var response = new CustomerEligibilityController(eligibilityInput, mapper)
                .getCustomerEligibilityByNumber(10_000_001L, null)
                .block();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("VIP", response.getBody().getProfile().name());
        assertEquals(true, response.getBody().getEligible());
    }

    @Test
    void returnsEligibilityByDocument() {
        CustomerEligibilityDto eligibility = new CustomerEligibilityDto(
                10_000_001L, CustomerType.PERSONAL, CustomerProfile.STANDARD, CustomerStatus.INACTIVE, false);
        when(eligibilityInput.getEligibility(DocumentType.DNI, "12345678"))
                .thenReturn(Single.just(eligibility));

        var response = new CustomerEligibilityController(eligibilityInput, mapper)
                .getCustomerEligibility(
                        com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.DocumentType.DNI,
                        "12345678",
                        null)
                .block();

        assertEquals("INACTIVE", response.getBody().getStatus().name());
        assertEquals(false, response.getBody().getEligible());
    }

    private CustomerDto customerDto() {
        return new CustomerDto(
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
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                java.time.Instant.parse("2026-01-01T00:00:00Z"));
    }

    private com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CreateCustomerRequest
            createRequest() {
        return new com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CreateCustomerRequest()
                .customerType(
                        com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerType.PERSONAL)
                .documentType(
                        com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.DocumentType.DNI)
                .documentNumber("12345678")
                .firstName("Ana")
                .lastName("Lopez")
                .email("ana@example.com")
                .phone("5551234");
    }
}
