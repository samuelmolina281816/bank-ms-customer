package com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("customer_contact")
@Getter
@Setter
public class CustomerContactEntity implements Persistable<Long> {
    @Id
    @Column("customer_id")
    private Long customerId;
    private String email;
    private String phone;
    private String address;
    @Transient
    private boolean newRecord;

    @Override
    public Long getId() {
        return customerId;
    }

    @Override
    public boolean isNew() {
        return newRecord;
    }
}
