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
public class SchedulePaymentTool {

    private final WalletClient walletClient;
    private final UserSearchClient userSearchClient;
    private final AgentFeaturesClient agentFeaturesClient;

    @Tool(description = """
            Schedules a one-time payment for the future. Can optionally be contingent on a minimum incoming credit condition (e.g. if salary is credited first).
            Required parameters:
            - senderUserId: the ID of the authenticated user sending the money
            - receiverNameOrId: the name of the recipient (e.g. "Mom", "John") or their numeric user ID
            - amount: the money amount to transfer
            - timeExpression: when to execute the payment (e.g. "tomorrow", "next Monday", or "2026-07-20T10:00:00")
            - conditionMinAmount: (optional) minimum credit amount to expect before execution (e.g. ₹20000 representing salary)
            - reminderMessage: (optional) custom text warning to alert the user if the credit condition was not met by the scheduled time
            Example: "Pay my electricity bill of 1000 tomorrow if my salary of 20000 is credited. If not, remind me."
            """)
    public String schedulePayment(
            Long senderUserId,
            String receiverNameOrId,
            BigDecimal amount,
            String timeExpression,
            BigDecimal conditionMinAmount,
            String reminderMessage) {

        log.info("[SchedulePaymentTool] Scheduling request: sender={}, receiver={}, amount={}, time={}",
                senderUserId, receiverNameOrId, amount, timeExpression);

        String bearerToken = extractBearerToken();

        try {
            // 1. Resolve Sender Wallet
            WalletClient.WalletInfo senderWallet = walletClient.getWalletByUserId(senderUserId, bearerToken);
            if (senderWallet == null) {
                return "Scheduling failed: Could not resolve your wallet. Please contact support.";
            }

            // 2. Resolve Receiver User ID
            Long receiverUserId = userSearchClient.resolveUserByName(receiverNameOrId, bearerToken);
            if (receiverUserId == null) {
                // If not found, list available users to help the agent/user choose
                List<UserSearchClient.UserDto> users = userSearchClient.getAllUsers(bearerToken);
                String available = (users == null || users.isEmpty()) ? "None" :
                        users.stream()
                                .map(u -> String.format("%s (ID: %d)", u.name(), u.id()))
                                .collect(Collectors.joining(", "));
                return String.format("Scheduling failed: Could not find user '%s'. Available users: [%s]. Please check the name or provide a numeric user ID.",
                        receiverNameOrId, available);
            }

            // 3. Resolve Receiver Wallet
            WalletClient.WalletInfo receiverWallet = walletClient.getWalletByUserId(receiverUserId, bearerToken);
            if (receiverWallet == null) {
                return String.format("Scheduling failed: Recipient user ID %d does not have an active wallet.", receiverUserId);
            }

            // Prevent Self-Scheduling
            if (senderWallet.id().equals(receiverWallet.id())) {
                return "Scheduling failed: You cannot schedule a transfer to yourself.";
            }

            // 4. Parse Time Expression
            LocalDateTime executionTime = parseTimeExpression(timeExpression);

            // 5. Call Client
            String description = String.format("Scheduled transfer to '%s' (User ID %d)", receiverNameOrId, receiverUserId);
            AgentFeaturesClient.ScheduledPaymentInfo info = agentFeaturesClient.createScheduledPayment(
                    senderWallet.id(),
                    receiverWallet.id(),
                    amount,
                    description,
                    executionTime,
                    conditionMinAmount,
                    reminderMessage,
                    bearerToken
            );

            String condMsg = conditionMinAmount != null ?
                    String.format(" (Condition: Pending incoming credit of at least ₹%.2f)", conditionMinAmount) : "";

            return String.format(
                    "📅 Payment scheduled successfully! ID: %d. Amount: ₹%.2f will be transferred to user '%s' (ID %d) at %s.%s",
                    info.id(), amount, receiverNameOrId, receiverUserId, executionTime, condMsg
            );

        } catch (Exception e) {
            log.error("[SchedulePaymentTool] Error: {}", e.getMessage());
            return "Scheduling failed: " + e.getMessage() + ". Please try again.";
        }
    }

    private LocalDateTime parseTimeExpression(String expression) {
        String clean = expression.toLowerCase().trim();
        LocalDateTime now = LocalDateTime.now();

        if (clean.contains("tomorrow")) {
            return now.plusDays(1);
        } else if (clean.contains("next week")) {
            return now.plusWeeks(1);
        } else if (clean.contains("next monday")) {
            LocalDateTime nextMonday = now;
            while (nextMonday.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
                nextMonday = nextMonday.plusDays(1);
            }
            return nextMonday.withHour(10).withMinute(0).withSecond(0);
        }

        // Try standard ISO parsing
        try {
            return LocalDateTime.parse(expression);
        } catch (Exception ignored) {}

        // Fallback: Default to tomorrow if parsing fails
        return now.plusDays(1);
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
