package com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter;

import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.PersonalCustomerEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SpringDataPersonalCustomerRepository
        extends ReactiveCrudRepository<PersonalCustomerEntity, Long> {
}
