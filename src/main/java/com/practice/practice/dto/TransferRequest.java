package com.practice.practice.dto;

import lombok.Data;

@Data
public class TransferRequest {

    private Long senderWalletId;
    private Long receiverWalletId;
    private Double amount;
}
