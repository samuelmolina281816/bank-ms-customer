package com.nttdata.bank_ms_customer.application.usecase;

import com.nttdata.bank_ms_customer.application.dto.CreateCustomerCommand;
import com.nttdata.bank_ms_customer.application.dto.CustomerDto;
import com.nttdata.bank_ms_customer.application.dto.CustomerEligibilityDto;
import com.nttdata.bank_ms_customer.application.dto.UpdateCustomerCommand;
import com.nttdata.bank_ms_customer.application.port.input.CustomerEligibilityInputPort;
import com.nttdata.bank_ms_customer.application.port.input.CustomerInputPort;
import com.nttdata.bank_ms_customer.application.port.output.CustomerNumberGenerator;
import com.nttdata.bank_ms_customer.application.port.output.CustomerRepositoryPort;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.domain.exception.CustomerNotFoundException;
import com.nttdata.bank_ms_customer.domain.exception.DuplicateCustomerDocumentException;
import com.nttdata.bank_ms_customer.domain.model.ContactInformation;
import com.nttdata.bank_ms_customer.domain.model.Customer;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.util.Objects;

public final class CustomerUseCase implements CustomerInputPort, CustomerEligibilityInputPort {
    private final CustomerRepositoryPort customerRepository;
    private final CustomerNumberGenerator customerNumberGenerator;

    public CustomerUseCase(CustomerRepositoryPort customerRepository, CustomerNumberGenerator customerNumberGenerator) {
        this.customerRepository = Objects.requireNonNull(customerRepository);
        this.customerNumberGenerator = Objects.requireNonNull(customerNumberGenerator);
    }

    @Override
    public Single<CustomerDto> create(CreateCustomerCommand command) {
        return Single.defer(() -> {
            IdentificationDocument document = new IdentificationDocument(command.documentType(), command.documentNumber());
            ContactInformation contact = new ContactInformation(command.email(), command.phone(), command.address());
            Customer.validateIdentity(command.customerType(), document,
                    command.firstName(), command.lastName(), command.businessName());
            return customerRepository.existsByDocument(document)
                    .flatMap(exists -> exists
                            ? Single.error(new DuplicateCustomerDocumentException())
                            : customerNumberGenerator.nextCustomerNumber())
                    .map(number -> Customer.create(number, command.customerType(), document, contact,
                            command.firstName(), command.lastName(), command.businessName(), Instant.now()))
                    .flatMap(customerRepository::save)
                    .map(CustomerDto::from);
        });
    }

    @Override
    public Maybe<CustomerDto> findByCustomerNumber(Long customerNumber) {
        return customerRepository.findByCustomerNumber(customerNumber)
                .map(CustomerDto::from);
    }

    @Override
    public Maybe<CustomerDto> findByDocument(DocumentType type, String number) {
        return customerRepository.findByDocument(new IdentificationDocument(type, number))
                .map(CustomerDto::from);
    }

    @Override
    public Flowable<CustomerDto> findAll() {
        return customerRepository.findAll().map(CustomerDto::from);
    }

    @Override
    public Single<CustomerDto> update(Long customerNumber, UpdateCustomerCommand command) {
        return customerRepository.findByCustomerNumber(customerNumber)
                .switchIfEmpty(Maybe.error(new CustomerNotFoundException()))
                .flatMapSingle(customer -> {
                    customer.update(command.firstName(), command.lastName(), command.businessName(),
                            command.email(), command.phone(), command.address(), Instant.now());
                    return customerRepository.save(customer);
                })
                .map(CustomerDto::from)
                .toSingle();
    }

    @Override
    public Completable deactivate(Long customerNumber) {
        return customerRepository.findByCustomerNumber(customerNumber)
                .switchIfEmpty(Maybe.error(new CustomerNotFoundException()))
                .flatMapSingle(customer -> {
                    customer.deactivate(Instant.now());
                    return customerRepository.save(customer);
                })
                .ignoreElement();
    }

    @Override
    public Single<CustomerEligibilityDto> getEligibility(DocumentType type, String number) {
        return customerRepository.findByDocument(new IdentificationDocument(type, number))
                .switchIfEmpty(Maybe.error(new CustomerNotFoundException()))
                .map(customer -> new CustomerEligibilityDto(
                        customer.getCustomerNumber(),
                        customer.getCustomerType(),
                        customer.getProfile(),
                        customer.getStatus(),
                        customer.isActive()))
                .toSingle();
    }
}
