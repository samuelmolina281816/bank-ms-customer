package com.nttdata.bank_ms_customer.framework.adapter.input.rest.controller;

import com.nttdata.bank_ms_customer.application.port.input.CustomerEligibilityInputPort;
import com.nttdata.bank_ms_customer.domain.enums.DocumentType;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.api.CustomerEligibilityApi;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerEligibilityResponse;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.mapper.CustomerRestMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class CustomerEligibilityController implements CustomerEligibilityApi {
    private final CustomerEligibilityInputPort eligibilityInput;
    private final CustomerRestMapper mapper;

    public CustomerEligibilityController(CustomerEligibilityInputPort eligibilityInput, CustomerRestMapper mapper) {
        this.eligibilityInput = eligibilityInput;
        this.mapper = mapper;
    }

    @Override
    public Mono<ResponseEntity<CustomerEligibilityResponse>> getCustomerEligibility(
            com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.DocumentType documentType,
            String documentNumber,
            ServerWebExchange exchange) {
        return Mono.from(eligibilityInput.getEligibility(
                        DocumentType.valueOf(documentType.name()), documentNumber).toFlowable())
                .map(eligibility -> ResponseEntity.ok(mapper.toEligibilityResponse(eligibility)));
    }
}
