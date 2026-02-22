package com.practice.practice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import com.practice.practice.enumss.TransactionType;
import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private String status;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Many transactions → one sender wallet
    @ManyToOne
    @JoinColumn(name = "sender_wallet_id")
    @JsonIgnore
    private WalletEntity senderWallet;

    // Many transactions → one receiver wallet
    @ManyToOne
    @JoinColumn(name = "receiver_wallet_id")
    @JsonIgnore
    private WalletEntity receiverWallet;
}

// package com.practice.practice.dto;

// import lombok.Data;
// import java.math.BigDecimal;

// @Data
// public class DepositRequest {

// private Long userId;
// private BigDecimal amount;
// }
