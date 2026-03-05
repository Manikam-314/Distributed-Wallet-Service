package com.programming.techie.repository;

import com.programming.techie.entity.LedgerEntry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT COALESCE(SUM(l.amount),0) FROM LedgerEntry l WHERE l.walletId = :walletId")
    BigDecimal calculateBalance(@Param("walletId") Long walletId);
}
