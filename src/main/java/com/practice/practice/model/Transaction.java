package com.practice.practice.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@Entity
@Table(name="transaction")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    private String type;

    private String status;
        private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
     // Many transactions → one sender wallet
    @ManyToOne
    @JoinColumn(name = "sender_wallet_id")
    private WalletEntity senderWallet;

    // Many transactions → one receiver wallet
    @ManyToOne
    @JoinColumn(name = "receiver_wallet_id")
    private WalletEntity receiverWallet;
}




// package com.practice.practice.dto;

// import lombok.Data;
// import java.math.BigDecimal;

// @Data
// public class DepositRequest {

//     private Long userId;
//     private BigDecimal amount;
// }
