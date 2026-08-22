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
@Table(name = "scheduled_payments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long senderWalletId;
    private Long receiverWalletId;
    private BigDecimal amount;
    private String description;
    private LocalDateTime executionTime;
    
    private BigDecimal conditionMinAmount;
    private boolean conditionMet;
    private String reminderMessage;
    
    private String status; // PENDING, COMPLETED, FAILED, REMINDED
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
