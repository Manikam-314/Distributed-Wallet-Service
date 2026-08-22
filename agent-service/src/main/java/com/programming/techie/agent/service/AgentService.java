package com.programming.techie.agent.service;

import com.programming.techie.agent.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AgentService — Core AI orchestration layer.
 *
 * Flow:
 *   Controller → AgentService → ChatClient (Gemini) → Tool Calling Engine
 *
 * Responsibilities:
 *   1. Inject authenticated userId into the system prompt (never trust the frontend)
 *   2. Detect high-risk actions (transfer) and return a confirmation request
 *   3. On confirmation, release the pending action for execution
 *   4. Publish AgentExecutedEvent to Kafka for audit (no local DB writes)
 *   5. Return structured, explainable responses
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final ChatClient chatClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ToolCallback[] financialTools;

    private static final String KAFKA_AUDIT_TOPIC = "ai-audit-logs";

    // ── High-risk keywords that require user confirmation before execution ──
    private static final java.util.List<String> HIGH_RISK_KEYWORDS =
            java.util.List.of("transfer", "send", "pay", "move money", "debit");

    public ChatResponse processChat(Long userId, String prompt) {
        long startTime = System.currentTimeMillis();
        String requestId = "AI-REQ-" + UUID.randomUUID();

        log.info("[AgentService] Processing chat: userId={}, requestId={}, prompt='{}'",
                userId, requestId, prompt);

        // ── Confirmation Guard: intercept high-risk actions BEFORE sending to LLM ──
        if (isHighRiskAction(prompt)) {
            log.info("[AgentService] High-risk prompt detected. Sending confirmation request. requestId={}", requestId);

            String confirmationMessage = String.format(
                    "🔐 **Confirmation Required**\n\nI understood your request:\n\n> \"%s\"\n\n" +
                    "This is a financial action that cannot be undone. Please confirm by replying **\"Yes, confirm\"** " +
                    "or cancel by replying **\"No, cancel\"**.\n\n_Request ID: %s_",
                    prompt, requestId
            );

            publishAuditEvent(userId, prompt, "CONFIRMATION_PENDING", requestId);

            return ChatResponse.builder()
                    .response(confirmationMessage)
                    .requestId(requestId)
                    .confirmationRequired(true)
                    .status("CONFIRMATION_PENDING")
                    .reason("High-risk financial action requires explicit user confirmation")
                    .executionTime((System.currentTimeMillis() - startTime) + "ms")
                    .build();
        }

        // ── Low-risk: pass directly to Gemini with all tools ──
        return executeWithLLM(userId, prompt, requestId, startTime);
    }

    public ChatResponse processConfirmation(Long userId, String prompt) {
        long startTime = System.currentTimeMillis();
        String requestId = "AI-REQ-" + UUID.randomUUID();

        log.info("[AgentService] Processing confirmation: userId={}, requestId={}, prompt='{}'",
                userId, requestId, prompt);

        // ── User said YES → proceed with full tool execution ──
        return executeWithLLM(userId, prompt, requestId, startTime);
    }

    // ─────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────

    private ChatResponse executeWithLLM(Long userId, String prompt, String requestId, long startTime) {
        // System prompt: inject authenticated userId so tools know who is acting
        String systemPrompt = String.format("""
                You are a secure and helpful financial AI agent for a distributed wallet platform.
                
                AUTHENTICATED USER ID: %d
                
                CRITICAL RULES:
                1. Always use userId=%d when calling tools.
                2. NEVER hallucinate or generate placeholder balances (e.g., "[balance amount]").
                3. If the user asks for their balance, YOU MUST EXECUTE the getBalance tool.
                4. If a tool returns an error, tell the user the error.
                5. Always use Indian Rupee (₹) for amounts.
                """, userId, userId);

        try {
            String aiText = chatClient.prompt()
                    .system(systemPrompt)
                    .user(prompt)
                    .tools(financialTools)  // inject all registered Phase 1 tools
                    .call()
                    .content();

            long executionTime = System.currentTimeMillis() - startTime;
            publishAuditEvent(userId, prompt, "SUCCESS", requestId);

            log.info("[AgentService] LLM responded in {}ms for userId={}", executionTime, userId);

            return ChatResponse.builder()
                    .response(aiText)
                    .requestId(requestId)
                    .status("SUCCESS")
                    .executionTime(executionTime + "ms")
                    .confirmationRequired(false)
                    .build();

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[AgentService] LLM call failed: userId={}, error={}", userId, e.getMessage());
            publishAuditEvent(userId, prompt, "FAILED: " + e.getMessage(), requestId);

            return ChatResponse.builder()
                    .response("I encountered an error processing your request. Please try again.")
                    .requestId(requestId)
                    .status("FAILED")
                    .reason(e.getMessage())
                    .executionTime(executionTime + "ms")
                    .build();
        }
    }

    private boolean isHighRiskAction(String prompt) {
        String lower = prompt.toLowerCase();
        return HIGH_RISK_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * Publishes an audit event to Kafka.
     * The agent-service has NO local DB — audit is fully event-driven.
     * A downstream consumer (future audit-service or observability component) persists this.
     */
    private void publishAuditEvent(Long userId, String prompt, String result, String requestId) {
        try {
            Map<String, Object> auditEvent = new HashMap<>();
            auditEvent.put("eventType", "AgentExecutedEvent");
            auditEvent.put("requestId", requestId);
            auditEvent.put("userId", userId);
            auditEvent.put("prompt", prompt);
            auditEvent.put("result", result);
            auditEvent.put("timestamp", Instant.now().toString());

            kafkaTemplate.send(KAFKA_AUDIT_TOPIC, requestId, auditEvent);
            log.info("[AgentService] Audit event published. requestId={}", requestId);
        } catch (Exception e) {
            // Audit failure must NEVER affect the main flow
            log.warn("[AgentService] Failed to publish audit event: {}", e.getMessage());
        }
    }
}
