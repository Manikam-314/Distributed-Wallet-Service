package com.programming.techie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalTopic;

    @Column(length = 5000)
    private String payload;

    private String errorReason;

    private int retryCount;

    private Instant failedAt;
}
