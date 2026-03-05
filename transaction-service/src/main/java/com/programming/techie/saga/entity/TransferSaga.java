package com.programming.techie.saga.entity;

import com.programming.techie.saga.status.SagaStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "transfer_saga")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferSaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transactionId;

    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    private Instant createdAt;
    private Instant updatedAt;
}
