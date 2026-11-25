package com.nttdata.bank_ms_customer.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nttdata.bank_ms_customer.domain.enums.CustomerProfile;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.exception.DuplicateCustomerDocumentException;
import com.nttdata.bank_ms_customer.domain.model.ContactInformation;
import com.nttdata.bank_ms_customer.domain.model.Customer;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter.CustomerRepositoryAdapter;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter.SpringDataBusinessCustomerRepository;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter.SpringDataCustomerContactRepository;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter.SpringDataCustomerRepository;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter.SpringDataPersonalCustomerRepository;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.BusinessCustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.CustomerContactEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.CustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.PersonalCustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.mapper.CustomerPersistenceMapper;
import java.time.Instant;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CustomerRepositoryAdapterUnitTest {
    @Mock
    private SpringDataCustomerRepository customerRepository;
    @Mock
    private SpringDataPersonalCustomerRepository personalRepository;
    @Mock
    private SpringDataBusinessCustomerRepository businessRepository;
    @Mock
    private SpringDataCustomerContactRepository contactRepository;
    @Mock
    private TransactionalOperator transactionalOperator;
    @Mock
    private DatabaseClient databaseClient;

    private CustomerRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CustomerRepositoryAdapter(
                customerRepository,
                personalRepository,
                businessRepository,
                contactRepository,
                new CustomerPersistenceMapper(),
                transactionalOperator,
                databaseClient);
    }

    @Test
    void savesPersonalAggregateWithinTransaction() {
        passThroughTransactions();
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> {
            CustomerEntity entity = invocation.getArgument(0);
            entity.setId(25L);
            return Mono.just(entity);
        });
        when(personalRepository.save(any(PersonalCustomerEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(contactRepository.save(any(CustomerContactEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Customer saved = adapter.save(personalCustomer()).blockingGet();

        assertEquals(25L, saved.getId());
        assertEquals(CustomerType.PERSONAL, saved.getCustomerType());
        assertEquals(CustomerProfile.VIP, saved.getProfile());
        verify(personalRepository).save(any(PersonalCustomerEntity.class));
        verify(contactRepository).save(any(CustomerContactEntity.class));
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void savesBusinessAggregateThroughBusinessSubtypeRepository() {
        passThroughTransactions();
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> {
            CustomerEntity entity = invocation.getArgument(0);
            entity.setId(26L);
            return Mono.just(entity);
        });
        when(businessRepository.save(any(BusinessCustomerEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(contactRepository.save(any(CustomerContactEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Customer saved = adapter.save(businessCustomer()).blockingGet();

        assertEquals(CustomerType.BUSINESS, saved.getCustomerType());
        assertEquals(CustomerProfile.PYME, saved.getProfile());
        assertEquals("Empresa", saved.getBusinessName());
        verify(businessRepository).save(any(BusinessCustomerEntity.class));
    }

    @Test
    void findsCustomerByPublicNumberUsingBoundParameter() {
        Customer expected = personalCustomer();
        DatabaseClient.GenericExecuteSpec executeSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<Customer> rows = mock(RowsFetchSpec.class);
        when(databaseClient.sql(anyString())).thenReturn(executeSpec);
        when(executeSpec.bind(anyString(), any())).thenReturn(executeSpec);
        when(executeSpec.map(any(BiFunction.class))).thenReturn(rows);
        when(rows.one()).thenReturn(Mono.just(expected));

        Customer result = adapter.findByCustomerNumber(10_000_001L).blockingGet();

        assertEquals(10_000_001L, result.getCustomerNumber());
        verify(executeSpec).bind("customerNumber", 10_000_001L);
    }

    @Test
    void listsCustomersFromAggregateQuery() {
        Customer expected = personalCustomer();
        DatabaseClient.GenericExecuteSpec executeSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<Customer> rows = mock(RowsFetchSpec.class);
        when(databaseClient.sql(anyString())).thenReturn(executeSpec);
        when(executeSpec.map(any(BiFunction.class))).thenReturn(rows);
        when(rows.all()).thenReturn(Flux.just(expected));

        Customer result = adapter.findAll().blockingFirst();

        assertEquals(10_000_001L, result.getCustomerNumber());
    }

    @Test
    void translatesDocumentUniqueConstraintToDomainException() {
        passThroughTransactions();
        when(customerRepository.save(any(CustomerEntity.class)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("uq_customers_document")));

        assertThrows(DuplicateCustomerDocumentException.class,
                () -> adapter.save(personalCustomer()).blockingGet());
    }

    @Test
    void preservesUnrelatedIntegrityViolations() {
        passThroughTransactions();
        DataIntegrityViolationException failure = new DataIntegrityViolationException("foreign key violation");
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(Mono.error(failure));

        DataIntegrityViolationException actual = assertThrows(
                DataIntegrityViolationException.class,
                () -> adapter.save(personalCustomer()).blockingGet());

        assertEquals(failure, actual);
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
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private Customer businessCustomer() {
        return Customer.create(
                10_000_002L,
                CustomerType.BUSINESS,
                new IdentificationDocument(DocumentType.RUC, "20123456789"),
                new ContactInformation("empresa@example.com", "5559876", null),
                CustomerProfile.PYME,
                null,
                null,
                "Empresa",
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private void passThroughTransactions() {
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
