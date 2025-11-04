package com.example.fruitmarket.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // builder sẽ tự lấy timeout từ spring.http.client.* ở trên
        return builder.build();
    }
}
