package com.programming.techie.saga.repository;

import com.programming.techie.saga.entity.TransferSaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferSagaRepository extends JpaRepository<TransferSaga, Long> {

    Optional<TransferSaga> findByTransactionId(Long transactionId);
}
