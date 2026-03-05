package com.programming.techie.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class WalletClient {

    private final RestTemplate restTemplate;

    // wallet-service URL
    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    public WalletClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get wallet balance from wallet-service
     */
    public BigDecimal getBalance(Long walletId) {

        String url = walletServiceUrl + "/wallet/balance?walletId=" + walletId;

        return restTemplate.getForObject(url, BigDecimal.class);
    }
}