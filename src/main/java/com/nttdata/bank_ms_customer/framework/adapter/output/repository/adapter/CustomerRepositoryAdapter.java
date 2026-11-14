package com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter;

import com.nttdata.bank_ms_customer.application.port.output.CustomerRepositoryPort;
import com.nttdata.bank_ms_customer.domain.enums.CustomerType;
import com.nttdata.bank_ms_customer.domain.exception.DuplicateCustomerDocumentException;
import com.nttdata.bank_ms_customer.domain.model.Customer;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.BusinessCustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.CustomerContactEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.CustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.PersonalCustomerEntity;
import com.nttdata.bank_ms_customer.framework.adapter.output.repository.mapper.CustomerPersistenceMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class CustomerRepositoryAdapter implements CustomerRepositoryPort {
    private static final String DOCUMENT_UNIQUE_CONSTRAINT = "uq_customers_document";
    private static final String SELECT_CUSTOMER_AGGREGATE = """
            SELECT c.id, c.customer_number, c.customer_type, c.document_type, c.document_number,
                   c.profile_code, c.status, c.created_at, c.updated_at,
                   pc.customer_id AS personal_customer_id, pc.first_name, pc.last_name,
                   bc.customer_id AS business_customer_id, bc.business_name,
                   cc.customer_id AS contact_customer_id, cc.email, cc.phone, cc.address
            FROM customers c
            LEFT JOIN personal_customer pc ON pc.customer_id = c.id
            LEFT JOIN business_customer bc ON bc.customer_id = c.id
            LEFT JOIN customer_contact cc ON cc.customer_id = c.id
            """;

    private final SpringDataCustomerRepository customerRepository;
    private final SpringDataPersonalCustomerRepository personalRepository;
    private final SpringDataBusinessCustomerRepository businessRepository;
    private final SpringDataCustomerContactRepository contactRepository;
    private final CustomerPersistenceMapper mapper;
    private final TransactionalOperator transactionalOperator;
    private final DatabaseClient databaseClient;

    public CustomerRepositoryAdapter(
            SpringDataCustomerRepository customerRepository,
            SpringDataPersonalCustomerRepository personalRepository,
            SpringDataBusinessCustomerRepository businessRepository,
            SpringDataCustomerContactRepository contactRepository,
            CustomerPersistenceMapper mapper,
            TransactionalOperator transactionalOperator,
            DatabaseClient databaseClient) {
        this.customerRepository = customerRepository;
        this.personalRepository = personalRepository;
        this.businessRepository = businessRepository;
        this.contactRepository = contactRepository;
        this.mapper = mapper;
        this.transactionalOperator = transactionalOperator;
        this.databaseClient = databaseClient;
    }

    @Override
    public Single<Customer> save(Customer customer) {
        boolean newCustomer = customer.getId() == null;
        Mono<Customer> saveAggregate = Mono.defer(() -> customerRepository
                .save(mapper.toCustomerEntity(customer))
                .flatMap(savedRoot -> saveSubtypeAndContact(customer, savedRoot, newCustomer)));

        return Single.fromPublisher(transactionalOperator.transactional(saveAggregate)
                .onErrorMap(DataIntegrityViolationException.class, this::mapIntegrityViolation));
    }

    @Override
    public Maybe<Customer> findByCustomerNumber(Long customerNumber) {
        return Maybe.fromPublisher(databaseClient.sql(SELECT_CUSTOMER_AGGREGATE
                        + " WHERE c.customer_number = :customerNumber")
                .bind("customerNumber", customerNumber)
                .map((row, metadata) -> mapper.toDomain(row))
                .one());
    }

    @Override
    public Maybe<Customer> findByDocument(IdentificationDocument document) {
        return Maybe.fromPublisher(databaseClient.sql(SELECT_CUSTOMER_AGGREGATE
                        + " WHERE c.document_type = :documentType AND c.document_number = :documentNumber")
                .bind("documentType", document.type().name())
                .bind("documentNumber", document.number())
                .map((row, metadata) -> mapper.toDomain(row))
                .one());
    }

    @Override
    public Flowable<Customer> findAll() {
        Flux<Customer> customers = databaseClient.sql(SELECT_CUSTOMER_AGGREGATE + " ORDER BY c.customer_number")
                .map((row, metadata) -> mapper.toDomain(row))
                .all();
        return Flowable.fromPublisher(customers);
    }

    @Override
    public Single<Boolean> existsByDocument(IdentificationDocument document) {
        return Single.fromPublisher(customerRepository.existsByDocumentTypeAndDocumentNumber(
                document.type().name(), document.number()));
    }

    private Mono<Customer> saveSubtypeAndContact(
            Customer customer, CustomerEntity savedRoot, boolean newCustomer) {
        customer.assignPersistenceId(savedRoot.getId());
        Mono<CustomerContactEntity> saveContact =
                contactRepository.save(mapper.toContactEntity(customer, newCustomer));

        if (customer.getCustomerType() == CustomerType.PERSONAL) {
            return personalRepository.save(mapper.toPersonalEntity(customer, newCustomer))
                    .flatMap(personal -> saveContact.map(contact ->
                            mapper.toDomain(savedRoot, personal, null, contact)));
        }

        return businessRepository.save(mapper.toBusinessEntity(customer, newCustomer))
                .flatMap(business -> saveContact.map(contact ->
                        mapper.toDomain(savedRoot, null, business, contact)));
    }

    private RuntimeException mapIntegrityViolation(DataIntegrityViolationException error) {
        if (error.getMessage() != null && error.getMessage().contains(DOCUMENT_UNIQUE_CONSTRAINT)) {
            return new DuplicateCustomerDocumentException();
        }
        return error;
    }
}
