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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionalTriggerTool {

    private final WalletClient walletClient;
    private final UserSearchClient userSearchClient;
    private final AgentFeaturesClient agentFeaturesClient;

    @Tool(description = """
            Registers a credit-event conditional trigger payment (e.g. transfer money automatically once a certain credit event like a salary is received).
            Required parameters:
            - senderUserId: the ID of the authenticated user whose wallet receives the credit and sends the payment
            - minAmount: the minimum incoming credit amount threshold to monitor for (e.g. ₹25000 representing salary credit)
            - receiverNameOrId: the name of the recipient (e.g. "Mom", "John") or their numeric user ID
            - amount: the amount of money to transfer when the credit trigger matches
            Example: "After salary comes (credit of 25000), send 5000 to Mom"
            """)
    public String registerConditionalTrigger(
            Long senderUserId,
            BigDecimal minAmount,
            String receiverNameOrId,
            BigDecimal amount) {

        log.info("[ConditionalTriggerTool] Trigger request: sender={}, minAmount={}, receiver={}, amount={}",
                senderUserId, minAmount, receiverNameOrId, amount);

        String bearerToken = extractBearerToken();

        try {
            // 1. Resolve Sender Wallet
            WalletClient.WalletInfo senderWallet = walletClient.getWalletByUserId(senderUserId, bearerToken);
            if (senderWallet == null) {
                return "Trigger creation failed: Could not resolve your wallet. Please contact support.";
            }

            // 2. Resolve Receiver User ID
            Long receiverUserId = userSearchClient.resolveUserByName(receiverNameOrId, bearerToken);
            if (receiverUserId == null) {
                List<UserSearchClient.UserDto> users = userSearchClient.getAllUsers(bearerToken);
                String available = (users == null || users.isEmpty()) ? "None" :
                        users.stream()
                                .map(u -> String.format("%s (ID: %d)", u.name(), u.id()))
                                .collect(Collectors.joining(", "));
                return String.format("Trigger creation failed: Could not find user '%s'. Available users: [%s]. Please check the name or provide a numeric user ID.",
                        receiverNameOrId, available);
            }

            // 3. Resolve Receiver Wallet
            WalletClient.WalletInfo receiverWallet = walletClient.getWalletByUserId(receiverUserId, bearerToken);
            if (receiverWallet == null) {
                return String.format("Trigger creation failed: Recipient user ID %d does not have an active wallet.", receiverUserId);
            }

            // Prevent Self-Triggering
            if (senderWallet.id().equals(receiverWallet.id())) {
                return "Trigger creation failed: You cannot schedule triggers to transfer money to yourself.";
            }

            // 4. Call Client
            AgentFeaturesClient.ConditionalTriggerInfo info = agentFeaturesClient.createConditionalTrigger(
                    senderWallet.id(),
                    minAmount,
                    receiverWallet.id(),
                    amount,
                    bearerToken
            );

            return String.format(
                    "⚡ Conditional trigger registered successfully! ID: %d. Once an incoming credit of at least ₹%.2f is received on your wallet, ₹%.2f will be automatically sent to user '%s' (ID %d).",
                    info.id(), minAmount, amount, receiverNameOrId, receiverUserId
            );

        } catch (Exception e) {
            log.error("[ConditionalTriggerTool] Error: {}", e.getMessage());
            return "Trigger creation failed: " + e.getMessage() + ". Please try again.";
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
