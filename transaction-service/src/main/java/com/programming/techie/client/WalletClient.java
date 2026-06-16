package com.programming.techie.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class WalletClient {

    private static final Logger log = LoggerFactory.getLogger(WalletClient.class);
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
    @CircuitBreaker(name = "walletService", fallbackMethod = "getBalanceFallback")
    @Retry(name = "walletService")
    public BigDecimal getBalance(Long walletId) {
        String url = walletServiceUrl + "/wallet/balance?walletId=" + walletId;
        return restTemplate.getForObject(url, BigDecimal.class);
    }

    public BigDecimal getBalanceFallback(Long walletId, Throwable throwable) {
        log.error("Fallback triggered for getBalance due to: {}", throwable.getMessage());
        // Return 0 as fallback or handle appropriately
        return BigDecimal.ZERO;
    }

    @CircuitBreaker(name = "walletService", fallbackMethod = "getWalletFallback")
    @Retry(name = "walletService")
    public com.programming.techie.dto.WalletDTO getWallet(Long walletId) {
        String url = walletServiceUrl + "/wallet/" + walletId;
        return restTemplate.getForObject(url, com.programming.techie.dto.WalletDTO.class);
    }

    @CircuitBreaker(name = "walletService", fallbackMethod = "getWalletFallback")
    @Retry(name = "walletService")
    public com.programming.techie.dto.WalletDTO getWalletByUserId(Long userId) {
        String url = walletServiceUrl + "/wallet/by-user/" + userId;
        return restTemplate.getForObject(url, com.programming.techie.dto.WalletDTO.class);
    }

    public com.programming.techie.dto.WalletDTO getWalletFallback(Long walletId, Throwable throwable) {
        log.error("Fallback triggered for getWallet due to: {}", throwable.getMessage());
        throw new RuntimeException("Wallet service unavailable");
    }
}
