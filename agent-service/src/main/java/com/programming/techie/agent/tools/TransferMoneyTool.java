package com.programming.techie.agent.tools;

import com.programming.techie.agent.client.TransactionClient;
import com.programming.techie.agent.client.WalletClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TransferMoneyTool — Phase 1 MVP Tool ⭐
 *
 * High-Risk Action Tool. The LLM MUST NOT call this directly.
 * The AgentService intercepts the "transfer intent" and sends a CONFIRMATION
 * response to the user first. Only when the user confirms does the AgentService
 * call executePendingTransfer(), which invokes this tool.
 *
 * Safety Guarantees:
 * - Generates AI_REQUEST_ID as Idempotency-Key → prevents duplicate transfers
 * - Routes through API Gateway → transaction-service → Saga → Wallet
 * - Never touches the database directly
 * - ROLE_USER authorization enforced
 *
 * Authorization: ROLE_USER required.
 * Route: agent-service → API Gateway → transaction-service → Saga → Wallet
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferMoneyTool {

    private final WalletClient walletClient;
    private final TransactionClient transactionClient;

    @Tool(description = """
            Transfers money from the authenticated user's wallet to another user's wallet.
            Use this ONLY after the user has explicitly confirmed the transfer.
            Required parameters:
            - senderUserId: the ID of the authenticated user (sender)
            - receiverUserId: the ID of the recipient user
            - amount: the amount to transfer (must be positive)
            Example triggers: "Transfer ₹500 to user 2", "Send ₹1000 to John (userId 5)"
            IMPORTANT: Always verify the amount and recipient with the user before calling this tool.
            """)
    public String transferMoney(Long senderUserId, Long receiverUserId, BigDecimal amount) {

        log.info("[TransferMoneyTool] Initiating transfer: senderId={}, receiverId={}, amount={}",
                senderUserId, receiverUserId, amount);

        // ── Step 1: Validate amount ──
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Transfer failed: The amount must be a positive number.";
        }

        String bearerToken = extractBearerToken();

        try {
            // ── Step 2: Fetch sender wallet ──
            WalletClient.WalletInfo senderWallet = walletClient.getWalletByUserId(senderUserId, bearerToken);
            if (senderWallet == null) {
                return "Transfer failed: Could not find your wallet. Please contact support.";
            }

            // ── Step 3: Fetch receiver wallet ──
            WalletClient.WalletInfo receiverWallet = walletClient.getWalletByUserId(receiverUserId, bearerToken);
            if (receiverWallet == null) {
                return String.format("Transfer failed: No wallet found for user ID %d.", receiverUserId);
            }

            // ── Step 4: Generate AI_REQUEST_ID as idempotency key ──
            String idempotencyKey = "AI-REQ-" + UUID.randomUUID();
            log.info("[TransferMoneyTool] Generated Idempotency-Key: {}", idempotencyKey);

            // ── Step 5: Execute transfer via API Gateway → transaction-service ──
            String result = transactionClient.transfer(
                    senderWallet.id(),
                    receiverWallet.id(),
                    amount,
                    idempotencyKey,
                    bearerToken
            );

            log.info("[TransferMoneyTool] Transfer successful. Key={}", idempotencyKey);

            return String.format(
                    "✅ Transfer successful! ₹%.2f has been sent to user ID %d. " +
                    "Request ID: %s. The transaction is now being processed through our secure payment pipeline.",
                    amount, receiverUserId, idempotencyKey
            );

        } catch (Exception e) {
            log.error("[TransferMoneyTool] Transfer failed: senderId={}, receiverId={}, amount={}, error={}",
                    senderUserId, receiverUserId, amount, e.getMessage());
            return String.format("Transfer failed: %s. Please try again or contact support.", e.getMessage());
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
