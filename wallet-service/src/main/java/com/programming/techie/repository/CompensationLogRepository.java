package com.programming.techie.repository;

import com.programming.techie.entity.CompensationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompensationLogRepository extends JpaRepository<CompensationLog, Long> {
    Optional<CompensationLog> findByTransactionId(Long transactionId);
}
