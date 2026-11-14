package com.nttdata.bank_ms_customer.framework.adapter.output.repository.entity;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("customers")
@Getter
@Setter
public class CustomerEntity {
    @Id
    private Long id;
    @Column("customer_number")
    private Long customerNumber;
    @Column("customer_type")
    private String customerType;
    @Column("document_type")
    private String documentType;
    @Column("document_number")
    private String documentNumber;
    @Column("profile_code")
    private String profileCode;
    private String status;
    @Column("created_at")
    private Instant createdAt;
    @Column("updated_at")
    private Instant updatedAt;

}
