package com.programming.techie.agent.tools;

import com.programming.techie.agent.client.WalletClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;

/**
 * BalanceTool — Phase 1 MVP Tool
 *
 * Allows the LLM to fetch the current wallet balance for the authenticated user.
 * This is a low-risk, read-only operation — no confirmation required.
 *
 * Authorization: ROLE_USER required.
 * Route: agent-service → API Gateway → wallet-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceTool {

    private final WalletClient walletClient;

    public record BalanceRequest(Long userId) {}

    @Tool(description = """
            Fetches the current wallet balance for the authenticated user.
            You MUST call this tool when the user asks for their balance.
            Never generate a placeholder balance.
            """)
    public String getBalance(BalanceRequest request) {
        Long userId = request.userId();
        log.info("[BalanceTool] Fetching balance for userId={}", userId);

        String bearerToken = extractBearerToken();

        try {
            WalletClient.WalletInfo wallet = walletClient.getWalletByUserId(userId, bearerToken);

            if (wallet == null) {
                return "I couldn't find a wallet for your account. Please contact support.";
            }

            BigDecimal balance = walletClient.getBalance(wallet.id(), bearerToken);

            log.info("[BalanceTool] Balance for userId={} is ₹{}", userId, balance);

            return String.format(
                    "Your current wallet balance is ₹%.2f. (Wallet ID: %d)",
                    balance, wallet.id()
            );

        } catch (Exception e) {
            log.error("[BalanceTool] Failed for userId={}: {}", userId, e.getMessage());
            return "I was unable to fetch your balance at this time. Please try again shortly.";
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
