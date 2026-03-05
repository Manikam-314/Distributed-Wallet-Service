package com.programming.techie.inbox.service;

import com.programming.techie.inbox.entity.InboxEvent;
import com.programming.techie.inbox.repository.InboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxRepository inboxRepository;

    public boolean isDuplicate(String eventId) {
        return inboxRepository.existsById(eventId);
    }

    public void markProcessed(String eventId) {
        inboxRepository.save(new InboxEvent(eventId));
    }
}
