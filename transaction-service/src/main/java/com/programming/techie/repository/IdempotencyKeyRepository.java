package com.programming.techie.repository;

import com.programming.techie.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository
        extends JpaRepository<IdempotencyKey, Long> {

    // Spring automatically generates query
    boolean existsByKey(String key);
}