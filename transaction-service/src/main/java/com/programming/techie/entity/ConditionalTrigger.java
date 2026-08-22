package com.programming.techie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conditional_triggers")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConditionalTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long senderWalletId; // user's wallet monitored for credit events
    private BigDecimal minAmount; // credit threshold (e.g. salary amount)
    
    private Long receiverWalletId; // target transfer receiver wallet
    private BigDecimal amount; // target transfer amount
    
    private String status; // PENDING, TRIGGERED, FAILED
    private String failReason;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
}
