package com.practice.practice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.practice.practice.model.Transaction;
import com.practice.practice.model.*;  
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // transaction history of wallet
    List<Transaction> findBySenderWalletOrReceiverWallet(
            WalletEntity senderWallet,
            WalletEntity receiverWallet
    );
}

