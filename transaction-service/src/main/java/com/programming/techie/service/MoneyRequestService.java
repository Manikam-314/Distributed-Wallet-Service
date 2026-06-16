package com.programming.techie.service;

import com.programming.techie.dto.CreateMoneyRequestDto;
import com.programming.techie.entity.MoneyRequest;
import com.programming.techie.entity.RequestStatus;
import com.programming.techie.repository.MoneyRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import com.programming.techie.client.AuthClient;
import com.programming.techie.dto.UserDTO;
import com.programming.techie.events.NotificationEvent;
import org.springframework.kafka.core.KafkaTemplate;

@Service
@Slf4j
public class MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final TransactionService transactionService;
    private final AuthClient authClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final org.springframework.web.reactive.function.client.WebClient aiWebClient;
    private final com.programming.techie.client.WalletClient walletClient;

    public MoneyRequestService(
            MoneyRequestRepository moneyRequestRepository,
            TransactionService transactionService,
            AuthClient authClient,
            KafkaTemplate<String, Object> kafkaTemplate,
            com.programming.techie.client.WalletClient walletClient,
            @org.springframework.beans.factory.annotation.Value("${ai.service.url:http://localhost:8095}") String aiServiceUrl) {
        this.moneyRequestRepository = moneyRequestRepository;
        this.transactionService = transactionService;
        this.authClient = authClient;
        this.kafkaTemplate = kafkaTemplate;
        this.walletClient = walletClient;
        this.aiWebClient = org.springframework.web.reactive.function.client.WebClient.create(aiServiceUrl);
    }

    @Transactional
    public MoneyRequest createRequest(CreateMoneyRequestDto dto) {
        log.info("Creating money request from user {} to user {} for amount {}", 
                dto.getRequesterId(), dto.getRecipientId(), dto.getAmount());

        String extractedDueDate = null;
        String extractedIntent = null;

        try {
            // Call AI Service for extraction
            java.util.Map<String, Object> aiRequest = new java.util.HashMap<>();
            aiRequest.put("message", dto.getMessage());

            java.util.Map<String, Object> aiResponse = aiWebClient.post()
                    .uri("/ai/extract-reminder")
                    .bodyValue(aiRequest)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {})
                    .block(java.time.Duration.ofSeconds(5));

            if (aiResponse != null) {
                extractedDueDate = (String) aiResponse.get("dueDate");
                extractedIntent = (String) aiResponse.get("intent");
                if (extractedDueDate != null) {
                    log.info("AI Extracted meta → due={}, intent={}", extractedDueDate, extractedIntent);
                }
            }
        } catch (Exception e) {
            log.error("AI extraction failed: {}. Continuing without AI meta.", e.getMessage());
        }

        MoneyRequest request = MoneyRequest.builder()
                .requesterId(dto.getRequesterId())
                .recipientId(dto.getRecipientId())
                .amount(dto.getAmount())
                .message(dto.getMessage())
                .extractedDueDate(extractedDueDate)
                .extractedIntent(extractedIntent)
                .status(RequestStatus.PENDING)
                .build();
                
        MoneyRequest savedRequest = moneyRequestRepository.save(request);

        // Send Notification asynchronously
        try {
            UserDTO recipient = authClient.getUser(dto.getRecipientId());
            UserDTO requester = authClient.getUser(dto.getRequesterId());
            
            if (recipient != null) {
                NotificationEvent event = NotificationEvent.builder()
                        .mobileNumber(recipient.getMobileNumber())
                        .email(recipient.getEmail())
                        .message(String.format("Payment Request: %s has requested ₹%.2f. Message: %s", 
                                requester != null ? requester.getName() : "A user",
                                dto.getAmount(),
                                dto.getMessage()))
                        .deliveryChannel("BOTH")
                        .build();
                
                kafkaTemplate.send("notificationTopic", event);
            }
        } catch (Exception e) {
            log.error("Failed to send notification for money request: {}", e.getMessage());
        }

        return savedRequest;
    }

    public List<MoneyRequest> getPendingRequestsForUser(Long userId) {
        return moneyRequestRepository.findByRecipientIdAndStatusOrderByCreatedAtDesc(userId, RequestStatus.PENDING);
    }
    
    public List<MoneyRequest> getAllRequestsReceivedByUser(Long userId) {
        return moneyRequestRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }
    
    public List<MoneyRequest> getAllRequestsSentByUser(Long userId) {
        return moneyRequestRepository.findByRequesterIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void payRequest(Long requestId, Long payingUserId) {
        log.info("User {} paying money request {}", payingUserId, requestId);
        
        MoneyRequest request = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Money request not found"));
                
        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new RuntimeException("Request is not in PENDING state");
        }
        
        if (!request.getRecipientId().equals(payingUserId)) {
            throw new RuntimeException("User is not authorized to pay this request");
        }
        
        // Resolve wallets from User IDs
        com.programming.techie.dto.WalletDTO payingUserWallet = walletClient.getWalletByUserId(payingUserId);
        com.programming.techie.dto.WalletDTO requesterWallet = walletClient.getWalletByUserId(request.getRequesterId());

        if (payingUserWallet == null || requesterWallet == null) {
            throw new RuntimeException("Could not resolve wallets for the request");
        }

        // The recipient of the request is the SENDER of the money
        // The requester is the RECEIVER of the money
        String idempotencyKey = UUID.randomUUID().toString();
        
        transactionService.transfer(
                payingUserWallet.getId(), // sender
                requesterWallet.getId(),  // receiver
                request.getAmount(),            // amount
                idempotencyKey,                 // idempotency key
                payingUserId                    // loggedInUserId
        );
        
        // Update status
        request.setStatus(RequestStatus.PAID);
        moneyRequestRepository.save(request);
        
        log.info("Successfully paid money request {}", requestId);
    }

    @Transactional
    public void declineRequest(Long requestId, Long decliningUserId) {
        log.info("User {} declining money request {}", decliningUserId, requestId);
        
        MoneyRequest request = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Money request not found"));
                
        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new RuntimeException("Request is not in PENDING state");
        }
        
        if (!request.getRecipientId().equals(decliningUserId)) {
            throw new RuntimeException("User is not authorized to decline this request");
        }
        
        request.setStatus(RequestStatus.DECLINED);
        moneyRequestRepository.save(request);
    }
}
