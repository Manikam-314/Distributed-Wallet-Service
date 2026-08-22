package com.programming.techie.repository;

import com.programming.techie.entity.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {

    List<ScheduledPayment> findByStatusAndExecutionTimeLessThanEqual(String status, LocalDateTime time);

    List<ScheduledPayment> findBySenderWalletIdAndStatusAndConditionMinAmountIsNotNull(Long senderWalletId, String status);
    
    List<ScheduledPayment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
