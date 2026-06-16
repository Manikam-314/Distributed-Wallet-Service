package com.programming.techie.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletCompensationEvent {

    private Long transactionId;
    private Long walletId;
    private BigDecimal amount;
}

