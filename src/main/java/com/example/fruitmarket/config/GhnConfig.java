package com.example.fruitmarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GhnConfig {

    @Bean
    public WebClient ghnClient(
            @Value("${ghn.base-url:https://dev-online-gateway.ghn.vn/shiip/public-api}") String baseUrl,
            @Value("${ghn.token}") String token,
            @Value("${ghn.shop-id}") String shopId
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Token", token)
                .defaultHeader("ShopId", shopId)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
