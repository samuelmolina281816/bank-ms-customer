package com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter;

import com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity.BusinessCustomerEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SpringDataBusinessCustomerRepository
        extends ReactiveCrudRepository<BusinessCustomerEntity, Long> {
}
