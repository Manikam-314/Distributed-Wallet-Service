package com.programming.techie.inbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType; // TRANSACTION
    private String aggregateId;   // transaction id

    @Column(columnDefinition = "TEXT")
    private String payload;       // JSON event

    private String topic;

    private boolean published;

    private Instant createdAt;
}
