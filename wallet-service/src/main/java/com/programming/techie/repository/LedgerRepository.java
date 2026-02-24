package com.programming.techie.repository;

import com.programming.techie.model.LedgerEntry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT COALESCE(SUM(l.amount),0) FROM LedgerEntry l WHERE l.walletId = :walletId")
    Double calculateBalance(@Param("walletId") Long walletId);
}