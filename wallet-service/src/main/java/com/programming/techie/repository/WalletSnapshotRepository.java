package com.programming.techie.repository;

import com.programming.techie.entity.WalletSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletSnapshotRepository extends JpaRepository<WalletSnapshot, Long> {
    Optional<WalletSnapshot> findFirstByWalletIdOrderByLastEventIdDesc(Long walletId);
}
