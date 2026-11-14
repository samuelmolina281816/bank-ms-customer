package com.nttdata.bank_ms_customer.framework.adapter.input.rest.controller;

import com.nttdata.bank_ms_customer.application.port.input.CustomerInputPort;
import com.nttdata.bank_ms_customer.domain.exception.CustomerNotFoundException;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.api.CustomersApi;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CreateCustomerRequest;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.CustomerResponse;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.error.InvalidRequestException;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.UpdateCustomerRequest;
import com.nttdata.bank_ms_customer.framework.adapter.input.rest.mapper.CustomerRestMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class CustomerController implements CustomersApi {
    private final CustomerInputPort customerInput;
    private final CustomerRestMapper mapper;

    public CustomerController(CustomerInputPort customerInput, CustomerRestMapper mapper) {
        this.customerInput = customerInput;
        this.mapper = mapper;
    }

    @Override
    public Mono<ResponseEntity<CustomerResponse>> createCustomer(
            Mono<CreateCustomerRequest> createCustomerRequest,
            ServerWebExchange exchange) {
        return createCustomerRequest.flatMap(request -> Mono.from(customerInput.create(mapper.toCommand(request)).toFlowable())
                .map(customer -> ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(customer))));
    }

    @Override
    public Mono<ResponseEntity<Void>> deactivateCustomer(Long customerNumber, ServerWebExchange exchange) {
        return Mono.from(customerInput.deactivate(customerNumber).toFlowable())
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<Flux<CustomerResponse>>> findAllCustomers(ServerWebExchange exchange) {
        Flux<CustomerResponse> customers = Flux.from(customerInput.findAll().map(mapper::toResponse));
        return Mono.just(ResponseEntity.ok(customers));
    }

    @Override
    public Mono<ResponseEntity<CustomerResponse>> findCustomerByDocument(
            com.nttdata.bank_ms_customer.framework.adapter.input.rest.generated.model.DocumentType documentType,
            String documentNumber,
            ServerWebExchange exchange) {
        return Mono.from(customerInput.findByDocument(
                        com.nttdata.bank_ms_customer.domain.enums.DocumentType.valueOf(documentType.name()),
                        documentNumber).toFlowable())
                .switchIfEmpty(Mono.error(new CustomerNotFoundException()))
                .map(customer -> ResponseEntity.ok(mapper.toResponse(customer)));
    }

    @Override
    public Mono<ResponseEntity<CustomerResponse>> findCustomerByNumber(Long customerNumber, ServerWebExchange exchange) {
        return Mono.from(customerInput.findByCustomerNumber(customerNumber).toFlowable())
                .switchIfEmpty(Mono.error(new CustomerNotFoundException()))
                .map(customer -> ResponseEntity.ok(mapper.toResponse(customer)));
    }

    @Override
    public Mono<ResponseEntity<CustomerResponse>> updateCustomer(
            Long customerNumber,
            Mono<UpdateCustomerRequest> updateCustomerRequest,
            ServerWebExchange exchange) {
        return updateCustomerRequest.flatMap(request -> {
            if (request.getFirstName() == null && request.getLastName() == null
                    && request.getBusinessName() == null && request.getEmail() == null
                    && request.getPhone() == null && request.getAddress() == null) {
                return Mono.error(new InvalidRequestException("At least one customer field must be provided"));
            }
            return Mono.from(customerInput.update(customerNumber, mapper.toCommand(request)).toFlowable())
                    .map(customer -> ResponseEntity.ok(mapper.toResponse(customer)));
        });
    }
}
