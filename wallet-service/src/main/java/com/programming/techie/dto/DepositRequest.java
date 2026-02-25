package com.programming.techie.dto;

import lombok.Data;

@Data
public class DepositRequest {

    private Long walletId;
    private Double amount;
}
