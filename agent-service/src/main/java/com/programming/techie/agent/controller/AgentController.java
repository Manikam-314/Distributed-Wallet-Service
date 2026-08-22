package com.programming.techie.agent.controller;

import com.programming.techie.agent.dto.ChatRequest;
import com.programming.techie.agent.dto.ChatResponse;
import com.programming.techie.agent.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/chat")
    @RateLimiter(name = "agentChat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ChatResponse response = agentService.processChat(userId, request.getPrompt());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    @RateLimiter(name = "agentChat")
    public ResponseEntity<ChatResponse> confirm(@RequestBody ChatRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ChatResponse response = agentService.processConfirmation(userId, request.getPrompt());
        return ResponseEntity.ok(response);
    }
}
