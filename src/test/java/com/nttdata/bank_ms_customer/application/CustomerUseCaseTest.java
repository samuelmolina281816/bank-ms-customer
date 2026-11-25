package com.nttdata.bank_ms_customer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nttdata.bank_ms_customer.application.dto.CreateCustomerCommand;
import com.nttdata.bank_ms_customer.application.dto.CustomerEligibilityDto;
import com.nttdata.bank_ms_customer.application.dto.UpdateCustomerCommand;
import com.nttdata.bank_ms_customer.application.port.output.CustomerNumberGenerator;
import com.nttdata.bank_ms_customer.application.port.output.CustomerRepositoryPort;
import com.nttdata.bank_ms_customer.application.usecase.CustomerUseCase;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.exception.CustomerNotFoundException;
import com.nttdata.bank_ms_customer.domain.exception.DuplicateCustomerDocumentException;
import com.nttdata.bank_ms_customer.domain.exception.InvalidCustomerException;
import com.nttdata.bank_ms_customer.domain.model.ContactInformation;
import com.nttdata.bank_ms_customer.domain.model.Customer;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerUseCaseTest {
    @Mock
    private CustomerRepositoryPort customerRepository;
    @Mock
    private CustomerNumberGenerator customerNumberGenerator;

    private CustomerUseCase customerUseCase;

    @BeforeEach
    void setUp() {
        customerUseCase = new CustomerUseCase(customerRepository, customerNumberGenerator);
    }

    @Test
    void createsCustomerWithGeneratedPublicNumber() {
        when(customerRepository.existsByDocument(any())).thenReturn(Single.just(false));
        when(customerNumberGenerator.nextCustomerNumber()).thenReturn(Single.just(10_000_001L));
        when(customerRepository.save(any())).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        var result = customerUseCase.create(validPersonalCommand()).blockingGet();

        assertEquals(10_000_001L, result.customerNumber());
        assertEquals(com.nttdata.bank_ms_customer.domain.enums.CustomerStatus.ACTIVE, result.status());
        assertEquals(CustomerProfile.STANDARD, result.profile());
    }

    @Test
    void createsPersonalVipWhenRequested() {
        when(customerRepository.existsByDocument(any())).thenReturn(Single.just(false));
        when(customerNumberGenerator.nextCustomerNumber()).thenReturn(Single.just(10_000_001L));
        when(customerRepository.save(any())).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        var result = customerUseCase.create(validPersonalCommand(CustomerProfile.VIP)).blockingGet();

        assertEquals(CustomerProfile.VIP, result.profile());
    }

    @Test
    void rejectsDuplicateDocumentBeforeAllocatingCustomerNumber() {
        when(customerRepository.existsByDocument(any())).thenReturn(Single.just(true));

        assertThrows(DuplicateCustomerDocumentException.class,
                () -> customerUseCase.create(validPersonalCommand()).blockingGet());
    }

    @Test
    void returnsMinimalEligibilityByPublicCustomerNumber() {
        Customer customer = Customer.create(
                10_000_001L,
                CustomerType.PERSONAL,
                new IdentificationDocument(DocumentType.DNI, "12345678"),
                new ContactInformation("ana@example.com", "5551234", "Street"),
                CustomerProfile.VIP,
                "Ana",
                "Lopez",
                null,
                Instant.parse("2026-01-01T00:00:00Z"));
        when(customerRepository.findByCustomerNumber(10_000_001L)).thenReturn(Maybe.just(customer));

        CustomerEligibilityDto result = customerUseCase.getEligibilityByCustomerNumber(10_000_001L).blockingGet();

        assertEquals(10_000_001L, result.customerNumber());
        assertEquals(CustomerType.PERSONAL, result.customerType());
        assertEquals(CustomerProfile.VIP, result.profile());
        assertTrue(result.eligible());

        customer.deactivate(Instant.parse("2026-01-02T00:00:00Z"));
        CustomerEligibilityDto inactiveResult =
                customerUseCase.getEligibilityByCustomerNumber(10_000_001L).blockingGet();
        assertFalse(inactiveResult.eligible());
    }

    @Test
    void returnsNotFoundForUnknownPublicCustomerNumber() {
        when(customerRepository.findByCustomerNumber(10_000_001L)).thenReturn(Maybe.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerUseCase.getEligibilityByCustomerNumber(10_000_001L).blockingGet());
    }

    @Test
    void createsBusinessPymeWhenRequested() {
        when(customerRepository.existsByDocument(any())).thenReturn(Single.just(false));
        when(customerNumberGenerator.nextCustomerNumber()).thenReturn(Single.just(10_000_002L));
        when(customerRepository.save(any())).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        CreateCustomerCommand command = new CreateCustomerCommand(
                CustomerType.BUSINESS,
                DocumentType.RUC,
                "20123456789",
                null,
                null,
                "Empresa",
                "empresa@example.com",
                "5559876",
                null,
                CustomerProfile.PYME);

        var result = customerUseCase.create(command).blockingGet();

        assertEquals(CustomerType.BUSINESS, result.customerType());
        assertEquals(CustomerProfile.PYME, result.profile());
        assertEquals("Empresa", result.businessName());
    }

    @Test
    void rejectsInvalidProfileBeforeCheckingDocumentUniqueness() {
        assertThrows(InvalidCustomerException.class,
                () -> customerUseCase.create(validPersonalCommand(CustomerProfile.PYME)).blockingGet());

        verify(customerRepository, never()).existsByDocument(any());
        verify(customerNumberGenerator, never()).nextCustomerNumber();
    }

    @Test
    void propagatesNumberGeneratorFailureWithoutSaving() {
        RuntimeException failure = new IllegalStateException("sequence unavailable");
        when(customerRepository.existsByDocument(any())).thenReturn(Single.just(false));
        when(customerNumberGenerator.nextCustomerNumber()).thenReturn(Single.error(failure));

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> customerUseCase.create(validPersonalCommand()).blockingGet());

        assertSame(failure, actual);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void findsCustomersByNumberAndDocumentAndMapsThemToDtos() {
        Customer customer = personalCustomer();
        when(customerRepository.findByCustomerNumber(10_000_001L)).thenReturn(Maybe.just(customer));
        when(customerRepository.findByDocument(any())).thenReturn(Maybe.just(customer));

        var byNumber = customerUseCase.findByCustomerNumber(10_000_001L).blockingGet();
        var byDocument = customerUseCase.findByDocument(DocumentType.DNI, "12345678").blockingGet();

        assertEquals(10_000_001L, byNumber.customerNumber());
        assertEquals("12345678", byDocument.documentNumber());
        assertEquals(CustomerProfile.VIP, byNumber.profile());
    }

    @Test
    void listsAllCustomersAsDtos() {
        Customer customer = personalCustomer();
        when(customerRepository.findAll()).thenReturn(io.reactivex.rxjava3.core.Flowable.just(customer));

        List<com.nttdata.bank_ms_customer.application.dto.CustomerDto> results =
                customerUseCase.findAll().toList().blockingGet();

        assertEquals(1, results.size());
        assertEquals(10_000_001L, results.get(0).customerNumber());
    }

    @Test
    void updatesCustomerAndReturnsUpdatedDto() {
        Customer customer = personalCustomer();
        when(customerRepository.findByCustomerNumber(10_000_001L)).thenReturn(Maybe.just(customer));
        when(customerRepository.save(any())).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        var result = customerUseCase.update(
                10_000_001L,
                new UpdateCustomerCommand(null, null, null, "updated@example.com", null, null)).blockingGet();

        assertEquals("updated@example.com", result.email());
        assertEquals("Ana", result.firstName());
    }

    @Test
    void updateAndDeactivateReportMissingCustomers() {
        when(customerRepository.findByCustomerNumber(10_000_099L)).thenReturn(Maybe.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerUseCase.update(10_000_099L,
                        new UpdateCustomerCommand(null, null, null, null, null, null)).blockingGet());
        assertThrows(CustomerNotFoundException.class,
                () -> customerUseCase.deactivate(10_000_099L).blockingAwait());
    }

    @Test
    void deactivatesExistingCustomerAndPersistsInactiveState() {
        Customer customer = personalCustomer();
        when(customerRepository.findByCustomerNumber(10_000_001L)).thenReturn(Maybe.just(customer));
        when(customerRepository.save(any())).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        customerUseCase.deactivate(10_000_001L).blockingAwait();

        assertEquals(com.nttdata.bank_ms_customer.domain.enums.CustomerStatus.INACTIVE, customer.getStatus());
        verify(customerRepository).save(customer);
    }

    @Test
    void returnsEligibilityByDocumentAndNotFoundWhenDocumentIsUnknown() {
        Customer customer = personalCustomer();
        when(customerRepository.findByDocument(any()))
                .thenReturn(Maybe.just(customer))
                .thenReturn(Maybe.empty());

        CustomerEligibilityDto eligibility =
                customerUseCase.getEligibility(DocumentType.DNI, "12345678").blockingGet();

        assertTrue(eligibility.eligible());
        assertEquals(CustomerProfile.VIP, eligibility.profile());
        assertThrows(CustomerNotFoundException.class,
                () -> customerUseCase.getEligibility(DocumentType.DNI, "12345678").blockingGet());
    }

    private Customer personalCustomer() {
        return Customer.create(
                10_000_001L,
                CustomerType.PERSONAL,
                new IdentificationDocument(DocumentType.DNI, "12345678"),
                new ContactInformation("ana@example.com", "5551234", "Street"),
                CustomerProfile.VIP,
                "Ana",
                "Lopez",
                null,
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private CreateCustomerCommand validPersonalCommand() {
        return new CreateCustomerCommand(
                CustomerType.PERSONAL,
                DocumentType.DNI,
                "12345678",
                "Ana",
                "Lopez",
                null,
                "ana@example.com",
                "5551234",
                null,
                null);
    }

    private CreateCustomerCommand validPersonalCommand(CustomerProfile profile) {
        return new CreateCustomerCommand(
                CustomerType.PERSONAL,
                DocumentType.DNI,
                "12345678",
                "Ana",
                "Lopez",
                null,
                "ana@example.com",
                "5551234",
                null,
                profile);
    }
}
