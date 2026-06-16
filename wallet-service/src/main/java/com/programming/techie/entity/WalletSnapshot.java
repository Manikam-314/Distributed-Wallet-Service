package com.programming.techie.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long walletId;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    private Long lastEventId; // The ID of the last LedgerEntry included in this snapshot

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
