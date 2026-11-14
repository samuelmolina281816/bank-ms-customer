package com.nttdata.bank_ms_customer.application.port.input;

import com.nttdata.bank_ms_customer.application.dto.CustomerEligibilityDto;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import io.reactivex.rxjava3.core.Single;

public interface CustomerEligibilityInputPort {
    Single<CustomerEligibilityDto> getEligibility(DocumentType type, String number);
}
