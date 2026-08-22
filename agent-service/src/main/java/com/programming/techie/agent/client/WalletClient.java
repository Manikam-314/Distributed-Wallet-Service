package com.programming.techie.agent.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletClient {

    private final WebClient webClient;

    /**
     * Fetches wallet entity for a user.
     * Routes via API Gateway → wallet-service GET /wallet/by-user/{userId}
     */
    public WalletInfo getWalletByUserId(Long userId, String bearerToken) {
        try {
            return webClient.get()
                    .uri("/api/wallet/by-user/{userId}", userId)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .bodyToMono(WalletInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("[WalletClient] Failed to get wallet for userId={}: {}", userId, e.getMessage());
            throw new RuntimeException("Unable to fetch wallet information. Please try again.");
        }
    }

    /**
     * Fetches the current balance for a wallet.
     * Routes via API Gateway → wallet-service GET /wallet/balance?walletId={walletId}
     */
    public BigDecimal getBalance(Long walletId, String bearerToken) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/wallet/balance")
                            .queryParam("walletId", walletId)
                            .build())
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .bodyToMono(BigDecimal.class)
                    .block();
        } catch (Exception e) {
            log.error("[WalletClient] Failed to get balance for walletId={}: {}", walletId, e.getMessage());
            throw new RuntimeException("Unable to fetch balance. Please try again.");
        }
    }

    // Inner record to represent wallet info returned from wallet-service
    public record WalletInfo(Long id, Long userId, BigDecimal balance, Long version) {}
}
