package com.nttdata.bank_ms_customer.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nttdata.bank_ms_customer.framework.adapter.output.repository.adapter.PostgreSqlCustomerNumberGenerator;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Mono;

class CustomerNumberGeneratorUnitTest {
    @Test
    void returnsNextSequenceValue() {
        DatabaseClient databaseClient = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec executeSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<Long> rows = mock(RowsFetchSpec.class);
        when(databaseClient.sql(anyString())).thenReturn(executeSpec);
        when(executeSpec.map(any(BiFunction.class))).thenReturn(rows);
        when(rows.one()).thenReturn(Mono.just(10_000_001L));

        Long result = new PostgreSqlCustomerNumberGenerator(databaseClient).nextCustomerNumber().blockingGet();

        assertEquals(10_000_001L, result);
        verify(databaseClient).sql("SELECT nextval('customer_number_seq') AS value");
    }

    @Test
    void failsIfSequenceQueryReturnsNoValue() {
        DatabaseClient databaseClient = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec executeSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        @SuppressWarnings("unchecked")
        RowsFetchSpec<Long> rows = mock(RowsFetchSpec.class);
        when(databaseClient.sql(anyString())).thenReturn(executeSpec);
        when(executeSpec.map(any(BiFunction.class))).thenReturn(rows);
        when(rows.one()).thenReturn(Mono.empty());

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> new PostgreSqlCustomerNumberGenerator(databaseClient).nextCustomerNumber().blockingGet());

        assertEquals("Customer number sequence returned no value", failure.getMessage());
    }
}
