package com.debugassistant.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the REST client for Stack Overflow API calls
 */
@Configuration
public class StackOverflowClientConfig {

    @Bean(name = "stackOverflowRestClient")
    public RestClient stackOverflowRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.stackexchange.com/2.3")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}