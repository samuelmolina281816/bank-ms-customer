package com.nttdata.bank_ms_customer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nttdata.bank_ms_customer.application.dto.CreateCustomerCommand;
import com.nttdata.bank_ms_customer.application.port.output.CustomerNumberGenerator;
import com.nttdata.bank_ms_customer.application.port.output.CustomerRepositoryPort;
import com.nttdata.bank_ms_customer.application.usecase.CustomerUseCase;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.exception.DuplicateCustomerDocumentException;
import io.reactivex.rxjava3.core.Single;
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
    }

    @Test
    void rejectsDuplicateDocumentBeforeAllocatingCustomerNumber() {
        when(customerRepository.existsByDocument(any())).thenReturn(Single.just(true));

        assertThrows(DuplicateCustomerDocumentException.class,
                () -> customerUseCase.create(validPersonalCommand()).blockingGet());
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
                null);
    }
}
