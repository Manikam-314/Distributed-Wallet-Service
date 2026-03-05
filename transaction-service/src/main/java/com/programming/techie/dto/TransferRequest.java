package com.programming.techie.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TransferRequest {

    private Long senderWalletId;
    private Long receiverWalletId;

    private BigDecimal amount;}



