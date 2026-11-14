package com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter;

import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.CustomerEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface SpringDataCustomerRepository extends ReactiveCrudRepository<CustomerEntity, Long> {
    Mono<CustomerEntity> findByCustomerNumber(Long customerNumber);
    Mono<CustomerEntity> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);
    Mono<Boolean> existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);
}
