package com.programming.techie.repository;

import com.programming.techie.entity.ConditionalTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConditionalTriggerRepository extends JpaRepository<ConditionalTrigger, Long> {

    List<ConditionalTrigger> findBySenderWalletIdAndStatus(Long senderWalletId, String status);
    
    List<ConditionalTrigger> findByUserIdOrderByCreatedAtDesc(Long userId);
}
