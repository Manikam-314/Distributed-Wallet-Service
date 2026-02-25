package com.programming.techie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.programming.techie.entity.Transaction;
import com.programming.techie.entity.*;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // transaction history of wallet
    List<Transaction> findBySenderWalletIdOrReceiverWalletId
    (Long senderId, Long receiverId);

}

