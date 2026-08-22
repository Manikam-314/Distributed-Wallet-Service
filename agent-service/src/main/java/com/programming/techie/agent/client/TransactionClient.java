package com.programming.techie.agent.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionClient {

    private final WebClient webClient;

    /**
     * Executes a money transfer.
     * Routes via API Gateway → transaction-service POST /transactions/transfer
     * Passes the AI_REQUEST_ID as idempotency-key to prevent duplicate executions.
     */
    public String transfer(Long senderWalletId, Long receiverWalletId,
                           BigDecimal amount, String idempotencyKey, String bearerToken) {
        try {
            TransferPayload payload = new TransferPayload(senderWalletId, receiverWalletId, amount);
            return webClient.post()
                    .uri("/api/transactions/transfer")
                    .header("Authorization", bearerToken)
                    .header("idempotency-key", idempotencyKey) // AI_REQUEST_ID → prevents duplicate transfers
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("[TransactionClient] Transfer failed. Idempotency-Key={}, Error={}", idempotencyKey, e.getMessage());
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        }
    }

    /**
     * Fetches recent transaction history for a wallet.
     * Routes via API Gateway → transaction-service GET /transactions?walletId={walletId}
     */
    public List<TransactionSummary> getHistory(Long walletId, String bearerToken) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/transactions")
                            .queryParam("walletId", walletId)
                            .build())
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .bodyToFlux(TransactionSummary.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("[TransactionClient] Failed to fetch history for walletId={}: {}", walletId, e.getMessage());
            throw new RuntimeException("Unable to fetch transaction history. Please try again.");
        }
    }

    // ──── Inner Records (matches transaction-service entity fields) ────

    public record TransferPayload(Long senderWalletId, Long receiverWalletId, java.math.BigDecimal amount) {}

    public record TransactionSummary(
            Long id,
            Long senderWalletId,
            Long receiverWalletId,
            java.math.BigDecimal amount,
            String status,
            LocalDateTime createdAt
    ) {}
}
