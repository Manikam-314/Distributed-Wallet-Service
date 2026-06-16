package com.wallet.integrations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;

/**
 * Example Spring Boot Service connecting to the Python AI service.
 * Inject this into the TransactionService or RequestService.
 */
@Service
public class AiServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000/ai}")
    private String aiServiceUrl;

    public AiServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Data Transfer Object for Fraud Check API Request
     */
    public record FraudCheckReq(
            String userId,
            double amount,
            int txn_count_last_1min,
            int txn_count_last_10min,
            boolean location_change_flag,
            boolean device_change_flag,
            double time_of_day_hours
    ) {}

    /**
     * Data Transfer Object for Fraud Check API Response
     */
    public record FraudCheckRes(
            double fraudScore,
            String action,
            String reason
    ) {}

    /**
     * Calls the ML /fraud-check endpoint cleanly with timeouts and Correlation Headers.
     * Uses RestTemplate but can be swapped for WebClient.
     */
    public FraudCheckRes checkTransactionFraud(String userId, double amount, int transactionsLast10Min) {
        String url = aiServiceUrl + "/fraud-check";

        // Current hour of day
        LocalDateTime now = LocalDateTime.now();
        double hourOfDay = now.getHour() + (now.getMinute() / 60.0);

        FraudCheckReq requestDto = new FraudCheckReq(
                userId,
                amount,
                1, // Simplified count for the last 1min
                transactionsLast10Min,
                false, // Need to implement location diffing in standard geo logic
                false,
                hourOfDay
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", java.util.UUID.randomUUID().toString());

        HttpEntity<FraudCheckReq> entity = new HttpEntity<>(requestDto, headers);

        try {
            ResponseEntity<FraudCheckRes> response = restTemplate.postForEntity(url, entity, FraudCheckRes.class);
            return response.getBody();
        } catch (Exception e) {
            // Important: Failsafe mechanism.
            // If AI is offline, allow and log, or fail-closed based on risk appetite.
            System.err.println("AI Fraud Check Failed: " + e.getMessage());
            return new FraudCheckRes(0.0, "ALLOW", "AI Service Unreachable - Bypass");
        }
    }
}
