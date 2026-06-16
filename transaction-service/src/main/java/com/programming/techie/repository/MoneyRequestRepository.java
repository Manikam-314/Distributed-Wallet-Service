package com.programming.techie.repository;

import com.programming.techie.entity.MoneyRequest;
import com.programming.techie.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, Long> {

    List<MoneyRequest> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    
    List<MoneyRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    
    List<MoneyRequest> findByRecipientIdAndStatusOrderByCreatedAtDesc(Long recipientId, RequestStatus status);
    
    List<MoneyRequest> findByStatusAndExtractedDueDateStartingWith(RequestStatus status, String datePrefix);
    
    List<MoneyRequest> findByStatusAndReminderSentFalseAndExtractedDueDateLessThanEqual(RequestStatus status, String dateTime);
}
