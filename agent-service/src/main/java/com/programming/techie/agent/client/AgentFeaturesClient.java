package com.programming.techie.agent.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentFeaturesClient {

    private final WebClient webClient;

    public ScheduledPaymentInfo createScheduledPayment(
            Long senderWalletId,
            Long receiverWalletId,
            BigDecimal amount,
            String description,
            LocalDateTime executionTime,
            BigDecimal conditionMinAmount,
            String reminderMessage,
            String bearerToken) {
        
        try {
            ScheduledPayload payload = new ScheduledPayload(
                    senderWalletId, receiverWalletId, amount, description,
                    executionTime, conditionMinAmount, reminderMessage
            );

            return webClient.post()
                    .uri("/api/transactions/agent/schedule")
                    .header("Authorization", bearerToken)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(ScheduledPaymentInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("[AgentFeaturesClient] Failed to schedule payment: {}", e.getMessage());
            throw new RuntimeException("Scheduling failed: " + e.getMessage());
        }
    }

    public RecurringPaymentInfo createRecurringPayment(
            Long senderWalletId,
            Long receiverWalletId,
            BigDecimal amount,
            String description,
            String frequency,
            LocalDateTime nextExecutionTime,
            String bearerToken) {
        
        try {
            RecurringPayload payload = new RecurringPayload(
                    senderWalletId, receiverWalletId, amount, description, frequency, nextExecutionTime
            );

            return webClient.post()
                    .uri("/api/transactions/agent/recurring")
                    .header("Authorization", bearerToken)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(RecurringPaymentInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("[AgentFeaturesClient] Failed to schedule recurring payment: {}", e.getMessage());
            throw new RuntimeException("Recurring schedule failed: " + e.getMessage());
        }
    }

    public ConditionalTriggerInfo createConditionalTrigger(
            Long senderWalletId,
            BigDecimal minAmount,
            Long receiverWalletId,
            BigDecimal amount,
            String bearerToken) {
        
        try {
            ConditionalPayload payload = new ConditionalPayload(
                    senderWalletId, minAmount, receiverWalletId, amount
            );

            return webClient.post()
                    .uri("/api/transactions/agent/conditional")
                    .header("Authorization", bearerToken)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(ConditionalTriggerInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("[AgentFeaturesClient] Failed to create conditional trigger: {}", e.getMessage());
            throw new RuntimeException("Conditional trigger creation failed: " + e.getMessage());
        }
    }

    // DTOS and Inner Records
    public record ScheduledPayload(
            Long senderWalletId,
            Long receiverWalletId,
            BigDecimal amount,
            String description,
            LocalDateTime executionTime,
            BigDecimal conditionMinAmount,
            String reminderMessage
    ) {}

    public record RecurringPayload(
            Long senderWalletId,
            Long receiverWalletId,
            BigDecimal amount,
            String description,
            String frequency,
            LocalDateTime nextExecutionTime
    ) {}

    public record ConditionalPayload(
            Long senderWalletId,
            BigDecimal minAmount,
            Long receiverWalletId,
            BigDecimal amount
    ) {}

    public record ScheduledPaymentInfo(
            Long id,
            Long userId,
            Long senderWalletId,
            Long receiverWalletId,
            BigDecimal amount,
            String description,
            LocalDateTime executionTime,
            String status
    ) {}

    public record RecurringPaymentInfo(
            Long id,
            Long userId,
            Long senderWalletId,
            Long receiverWalletId,
            BigDecimal amount,
            String frequency,
            LocalDateTime nextExecutionTime,
            String status
    ) {}

    public record ConditionalTriggerInfo(
            Long id,
            Long userId,
            Long senderWalletId,
            BigDecimal minAmount,
            Long receiverWalletId,
            BigDecimal amount,
            String status
    ) {}
}
