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
@Table(name = "recurring_payments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long senderWalletId;
    private Long receiverWalletId;
    private BigDecimal amount;
    private String description;
    
    private String frequency; // DAILY, WEEKLY, MONTHLY
    private LocalDateTime nextExecutionTime;
    
    private String status; // ACTIVE, PAUSED, COMPLETED
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "ACTIVE";
        }
    }
}
