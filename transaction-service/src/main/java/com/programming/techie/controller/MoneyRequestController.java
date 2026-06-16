package com.programming.techie.controller;

import com.programming.techie.dto.CreateMoneyRequestDto;
import com.programming.techie.entity.MoneyRequest;
import com.programming.techie.service.MoneyRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;

    // POST /requests
    @PostMapping
    public ResponseEntity<MoneyRequest> createRequest(@RequestBody CreateMoneyRequestDto request) {
        return ResponseEntity.ok(moneyRequestService.createRequest(request));
    }

    // GET /requests/pending/{userId}
    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<MoneyRequest>> getPendingRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(moneyRequestService.getPendingRequestsForUser(userId));
    }

    // GET /requests/received/{userId}
    @GetMapping("/received/{userId}")
    public ResponseEntity<List<MoneyRequest>> getAllReceivedRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(moneyRequestService.getAllRequestsReceivedByUser(userId));
    }

    // GET /requests/sent/{userId}
    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<MoneyRequest>> getAllSentRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(moneyRequestService.getAllRequestsSentByUser(userId));
    }

    // POST /requests/{requestId}/pay/{userId}
    @PostMapping("/{requestId}/pay/{userId}")
    public ResponseEntity<String> payRequest(@PathVariable Long requestId, @PathVariable Long userId) {
        moneyRequestService.payRequest(requestId, userId);
        return ResponseEntity.ok("Request paid successfully");
    }

    // POST /requests/{requestId}/decline/{userId}
    @PostMapping("/{requestId}/decline/{userId}")
    public ResponseEntity<String> declineRequest(@PathVariable Long requestId, @PathVariable Long userId) {
        moneyRequestService.declineRequest(requestId, userId);
        return ResponseEntity.ok("Request declined successfully");
    }
}
