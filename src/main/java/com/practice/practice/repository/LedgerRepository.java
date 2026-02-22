package com.practice.practice.repository;

import com.practice.practice.model.LedgerEntry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT COALESCE(SUM(l.amount),0) FROM LedgerEntry l WHERE l.walletId = :walletId")
    Double calculateBalance(@Param("walletId") Long walletId);
}