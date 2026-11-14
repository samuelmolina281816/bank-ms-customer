package com.nttdata.bank_ms_customer.application.port.input;

import com.nttdata.bank_ms_customer.application.dto.CreateCustomerCommand;
import com.nttdata.bank_ms_customer.application.dto.CustomerDto;
import com.nttdata.bank_ms_customer.application.dto.UpdateCustomerCommand;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public interface CustomerInputPort {
    Single<CustomerDto> create(CreateCustomerCommand command);
    Maybe<CustomerDto> findByCustomerNumber(Long customerNumber);
    Maybe<CustomerDto> findByDocument(DocumentType type, String number);
    Flowable<CustomerDto> findAll();
    Single<CustomerDto> update(Long customerNumber, UpdateCustomerCommand command);
    Completable deactivate(Long customerNumber);
}
