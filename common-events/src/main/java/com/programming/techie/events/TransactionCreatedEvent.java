package com.programming.techie.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {

    private Long transactionId;
    private Long fromWalletId;
    private Long toWalletId;
    private Double amount;
    private Instant createdAt;
    private String idempotencyKey;
}

