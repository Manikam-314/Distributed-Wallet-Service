package com.programming.techie.controller;

import com.programming.techie.entity.ConditionalTrigger;
import com.programming.techie.entity.RecurringPayment;
import com.programming.techie.entity.ScheduledPayment;
import com.programming.techie.repository.ConditionalTriggerRepository;
import com.programming.techie.repository.RecurringPaymentRepository;
import com.programming.techie.repository.ScheduledPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentFeaturesController {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final RecurringPaymentRepository recurringPaymentRepository;
    private final ConditionalTriggerRepository conditionalTriggerRepository;

    @PostMapping("/schedule")
    public ResponseEntity<ScheduledPayment> createScheduledPayment(
            @RequestHeader("loggedInUserId") String loggedInUserId,
            @RequestBody ScheduledPayment scheduledPayment) {
        
        log.info("Agent scheduling payment for user {}: {}", loggedInUserId, scheduledPayment);
        scheduledPayment.setUserId(Long.parseLong(loggedInUserId));
        scheduledPayment.setStatus("PENDING");
        return ResponseEntity.ok(scheduledPaymentRepository.save(scheduledPayment));
    }

    @PostMapping("/recurring")
    public ResponseEntity<RecurringPayment> createRecurringPayment(
            @RequestHeader("loggedInUserId") String loggedInUserId,
            @RequestBody RecurringPayment recurringPayment) {
        
        log.info("Agent scheduling recurring payment for user {}: {}", loggedInUserId, recurringPayment);
        recurringPayment.setUserId(Long.parseLong(loggedInUserId));
        recurringPayment.setStatus("ACTIVE");
        return ResponseEntity.ok(recurringPaymentRepository.save(recurringPayment));
    }

    @PostMapping("/conditional")
    public ResponseEntity<ConditionalTrigger> createConditionalTrigger(
            @RequestHeader("loggedInUserId") String loggedInUserId,
            @RequestBody ConditionalTrigger conditionalTrigger) {
        
        log.info("Agent scheduling conditional trigger for user {}: {}", loggedInUserId, conditionalTrigger);
        conditionalTrigger.setUserId(Long.parseLong(loggedInUserId));
        conditionalTrigger.setStatus("PENDING");
        return ResponseEntity.ok(conditionalTriggerRepository.save(conditionalTrigger));
    }

    @GetMapping("/scheduled-list")
    public ResponseEntity<List<ScheduledPayment>> getScheduledPayments(
            @RequestHeader("loggedInUserId") String loggedInUserId) {
        return ResponseEntity.ok(scheduledPaymentRepository.findByUserIdOrderByCreatedAtDesc(Long.parseLong(loggedInUserId)));
    }

    @GetMapping("/recurring-list")
    public ResponseEntity<List<RecurringPayment>> getRecurringPayments(
            @RequestHeader("loggedInUserId") String loggedInUserId) {
        return ResponseEntity.ok(recurringPaymentRepository.findByUserIdOrderByCreatedAtDesc(Long.parseLong(loggedInUserId)));
    }

    @GetMapping("/conditional-list")
    public ResponseEntity<List<ConditionalTrigger>> getConditionalTriggers(
            @RequestHeader("loggedInUserId") String loggedInUserId) {
        return ResponseEntity.ok(conditionalTriggerRepository.findByUserIdOrderByCreatedAtDesc(Long.parseLong(loggedInUserId)));
    }

    @DeleteMapping("/scheduled/{id}")
    public ResponseEntity<String> cancelScheduledPayment(
            @RequestHeader("loggedInUserId") String loggedInUserId,
            @PathVariable Long id) {
        scheduledPaymentRepository.findById(id).ifPresent(sp -> {
            if (sp.getUserId().equals(Long.parseLong(loggedInUserId))) {
                scheduledPaymentRepository.delete(sp);
            }
        });
        return ResponseEntity.ok("Cancelled scheduled payment");
    }

    @DeleteMapping("/recurring/{id}")
    public ResponseEntity<String> cancelRecurringPayment(
            @RequestHeader("loggedInUserId") String loggedInUserId,
            @PathVariable Long id) {
        recurringPaymentRepository.findById(id).ifPresent(rp -> {
            if (rp.getUserId().equals(Long.parseLong(loggedInUserId))) {
                recurringPaymentRepository.delete(rp);
            }
        });
        return ResponseEntity.ok("Cancelled recurring payment");
    }

    @DeleteMapping("/conditional/{id}")
    public ResponseEntity<String> cancelConditionalTrigger(
            @RequestHeader("loggedInUserId") String loggedInUserId,
            @PathVariable Long id) {
        conditionalTriggerRepository.findById(id).ifPresent(ct -> {
            if (ct.getUserId().equals(Long.parseLong(loggedInUserId))) {
                conditionalTriggerRepository.delete(ct);
            }
        });
        return ResponseEntity.ok("Cancelled conditional trigger");
    }
}
