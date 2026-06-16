package com.programming.techie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "compensation_logs")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompensationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long transactionId;

    @Column(nullable = false)
    private Long walletId;

    @CreationTimestamp
    private Instant processedAt;
}
