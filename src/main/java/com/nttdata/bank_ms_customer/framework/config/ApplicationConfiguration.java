package com.nttdata.bank_ms_customer.framework.config;

import com.nttdata.bank_ms_customer.application.port.output.CustomerNumberGenerator;
import com.nttdata.bank_ms_customer.application.port.output.CustomerRepositoryPort;
import com.nttdata.bank_ms_customer.application.usecase.CustomerUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean
    public CustomerUseCase customerUseCase(
            CustomerRepositoryPort customerRepository,
            CustomerNumberGenerator customerNumberGenerator) {
        return new CustomerUseCase(customerRepository, customerNumberGenerator);
    }
}
