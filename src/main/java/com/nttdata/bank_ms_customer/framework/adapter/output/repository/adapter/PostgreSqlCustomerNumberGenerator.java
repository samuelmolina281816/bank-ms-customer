package com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter;

import com.nttdata.bank_ms_customer.application.port.output.CustomerNumberGenerator;
import io.reactivex.rxjava3.core.Single;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
public class PostgreSqlCustomerNumberGenerator implements CustomerNumberGenerator {
    private final DatabaseClient databaseClient;

    public PostgreSqlCustomerNumberGenerator(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Single<Long> nextCustomerNumber() {
        return Single.fromPublisher(databaseClient.sql("SELECT nextval('customer_number_seq') AS value")
                .map((row, metadata) -> row.get("value", Long.class))
                .one()
                .switchIfEmpty(reactor.core.publisher.Mono.error(
                        new IllegalStateException("Customer number sequence returned no value"))));
    }
}
