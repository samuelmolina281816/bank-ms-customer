package com.nttdata.bank_ms_customer.application.port.output;

import io.reactivex.rxjava3.core.Single;

public interface CustomerNumberGenerator {
    Single<Long> nextCustomerNumber();
}
