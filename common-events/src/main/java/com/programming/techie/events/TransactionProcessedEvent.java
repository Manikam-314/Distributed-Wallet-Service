package com.programming.techie.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionProcessedEvent {

    private Long transactionId;
    private String status; // SUCCESS / FAILED
    private String reason;
}

