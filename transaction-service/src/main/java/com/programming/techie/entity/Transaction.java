package com.programming.techie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import com.programming.techie.enums.TransactionType;
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
