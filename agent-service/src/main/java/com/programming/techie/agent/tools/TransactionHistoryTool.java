package com.programming.techie.agent.tools;

import com.programming.techie.agent.client.TransactionClient;
import com.programming.techie.agent.client.WalletClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TransactionHistoryTool — Phase 1 MVP Tool
 *
 * Allows the LLM to fetch the recent transaction history for the authenticated user.
 * This is a low-risk, read-only operation — no confirmation required.
 *
 * Authorization: ROLE_USER required.
 * Route: agent-service → API Gateway → transaction-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionHistoryTool {

    private final WalletClient walletClient;
    private final TransactionClient transactionClient;

    @Tool(description = """
            Fetches the recent transaction history for the authenticated user.
            Use this when the user asks: "Show my recent transactions", "What are my last payments?",
            "Show transaction history", "What did I spend recently?", or similar history inquiries.
            Returns a formatted summary of the most recent transactions.
            """)
    public String getTransactionHistory(Long userId) {
        log.info("[TransactionHistoryTool] Fetching history for userId={}", userId);

        String bearerToken = extractBearerToken();

        try {
            // Fetch wallet first to get walletId
            WalletClient.WalletInfo wallet = walletClient.getWalletByUserId(userId, bearerToken);

            if (wallet == null) {
                return "I couldn't find a wallet for your account. Please contact support.";
            }

            List<TransactionClient.TransactionSummary> history =
                    transactionClient.getHistory(wallet.id(), bearerToken);

            if (history == null || history.isEmpty()) {
                return "You have no recent transactions.";
            }

            // Format the last 10 transactions
            String formatted = history.stream()
                    .limit(10)
                    .map(tx -> String.format(
                            "  [%s] %s | ₹%.2f | %s → %s",
                            tx.createdAt() != null ? tx.createdAt().toLocalDate() : "N/A",
                            tx.status(),
                            tx.amount(),
                            tx.senderWalletId(),
                            tx.receiverWalletId()
                    ))
                    .collect(Collectors.joining("\n"));

            log.info("[TransactionHistoryTool] Found {} transactions for userId={}", history.size(), userId);

            return String.format("Here are your last %d transactions:\n\n%s",
                    Math.min(history.size(), 10), formatted);

        } catch (Exception e) {
            log.error("[TransactionHistoryTool] Failed for userId={}: {}", userId, e.getMessage());
            return "I was unable to retrieve your transaction history. Please try again shortly.";
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
