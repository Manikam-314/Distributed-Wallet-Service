package com.programming.techie.inbox.repository;

import com.programming.techie.inbox.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxRepository extends JpaRepository<InboxEvent, String> {
}
