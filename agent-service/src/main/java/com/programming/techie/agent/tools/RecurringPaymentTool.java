package com.programming.techie.agent.tools;

import com.programming.techie.agent.client.AgentFeaturesClient;
import com.programming.techie.agent.client.UserSearchClient;
import com.programming.techie.agent.client.WalletClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringPaymentTool {

    private final WalletClient walletClient;
    private final UserSearchClient userSearchClient;
    private final AgentFeaturesClient agentFeaturesClient;

    @Tool(description = """
            Schedules a recurring periodic payment (e.g. daily, weekly, or monthly transfer).
            Required parameters:
            - senderUserId: the ID of the authenticated user sending the money
            - receiverNameOrId: the name of the recipient (e.g. "Mom", "John") or their numeric user ID
            - amount: the money amount to transfer on each cycle
            - frequency: frequency of the payment (must be one of: "DAILY", "WEEKLY", "MONTHLY")
            - startTime: (optional) when the recurring schedule should start (defaults to immediately/today)
            Example: "Every month pay rent of 15000 to Landlord John"
            """)
    public String scheduleRecurringPayment(
            Long senderUserId,
            String receiverNameOrId,
            BigDecimal amount,
            String frequency,
            String startTime) {

        log.info("[RecurringPaymentTool] Recurring request: sender={}, receiver={}, amount={}, freq={}",
                senderUserId, receiverNameOrId, amount, frequency);

        String bearerToken = extractBearerToken();

        try {
            // 1. Validate Frequency
            if (frequency == null || (!frequency.equalsIgnoreCase("DAILY") && 
                    !frequency.equalsIgnoreCase("WEEKLY") && !frequency.equalsIgnoreCase("MONTHLY"))) {
                return "Recurring schedule failed: Invalid frequency. Please specify 'DAILY', 'WEEKLY', or 'MONTHLY'.";
            }

            // 2. Resolve Sender Wallet
            WalletClient.WalletInfo senderWallet = walletClient.getWalletByUserId(senderUserId, bearerToken);
            if (senderWallet == null) {
                return "Recurring schedule failed: Could not resolve your wallet. Please contact support.";
            }

            // 3. Resolve Receiver User ID
            Long receiverUserId = userSearchClient.resolveUserByName(receiverNameOrId, bearerToken);
            if (receiverUserId == null) {
                List<UserSearchClient.UserDto> users = userSearchClient.getAllUsers(bearerToken);
                String available = (users == null || users.isEmpty()) ? "None" :
                        users.stream()
                                .map(u -> String.format("%s (ID: %d)", u.name(), u.id()))
                                .collect(Collectors.joining(", "));
                return String.format("Recurring schedule failed: Could not find user '%s'. Available users: [%s]. Please check the name or provide a numeric user ID.",
                        receiverNameOrId, available);
            }

            // 4. Resolve Receiver Wallet
            WalletClient.WalletInfo receiverWallet = walletClient.getWalletByUserId(receiverUserId, bearerToken);
            if (receiverWallet == null) {
                return String.format("Recurring schedule failed: Recipient user ID %d does not have an active wallet.", receiverUserId);
            }

            // Prevent Self-Scheduling
            if (senderWallet.id().equals(receiverWallet.id())) {
                return "Recurring schedule failed: You cannot schedule recurring transfers to yourself.";
            }

            // 5. Parse Start Time
            LocalDateTime start = LocalDateTime.now();
            if (startTime != null && !startTime.trim().isEmpty()) {
                try {
                    start = LocalDateTime.parse(startTime);
                } catch (Exception ignored) {}
            }

            // 6. Call Client
            String description = String.format("Recurring monthly payment to '%s' (User ID %d)", receiverNameOrId, receiverUserId);
            AgentFeaturesClient.RecurringPaymentInfo info = agentFeaturesClient.createRecurringPayment(
                    senderWallet.id(),
                    receiverWallet.id(),
                    amount,
                    description,
                    frequency.toUpperCase(),
                    start,
                    bearerToken
            );

            return String.format(
                    "🔄 Recurring payment registered successfully! ID: %d. Every %s, ₹%.2f will be transferred to user '%s' (ID %d). First execution is scheduled for %s.",
                    info.id(), frequency.toLowerCase(), amount, receiverNameOrId, receiverUserId, start
            );

        } catch (Exception e) {
            log.error("[RecurringPaymentTool] Error: {}", e.getMessage());
            return "Recurring schedule failed: " + e.getMessage() + ". Please try again.";
        }
    }

    private String extractBearerToken() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String auth = attrs.getRequest().getHeader("Authorization");
            return auth != null ? auth : "";
        }
        return "";
    }
}
