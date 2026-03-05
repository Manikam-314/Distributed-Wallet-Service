package com.programming.techie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransactionResponse {

    private String transactionId;
    private String status;

    private Double amount;
    private String currency;

    private Long senderWalletId;
    private Long receiverWalletId;

    private Double senderBalanceAfter;
    private Double receiverBalanceAfter;

    private String idempotencyKey;

    private LocalDateTime createdAt;
}
