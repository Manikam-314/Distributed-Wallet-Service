package com.programming.techie.inbox.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class InboxEvent {

    @Id
    private String eventId;

    private Instant receivedAt = Instant.now();

    public InboxEvent() {}

    public InboxEvent(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
