package com.nttdata.bank_ms_customer.application.port.output;

import com.nttdata.bank_ms_customer.domain.model.Customer;
import com.nttdata.bank_ms_customer.domain.model.IdentificationDocument;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public interface CustomerRepositoryPort {
    Single<Customer> save(Customer customer);
    Maybe<Customer> findByCustomerNumber(Long customerNumber);
    Maybe<Customer> findByDocument(IdentificationDocument document);
    Flowable<Customer> findAll();
    Single<Boolean> existsByDocument(IdentificationDocument document);
}
