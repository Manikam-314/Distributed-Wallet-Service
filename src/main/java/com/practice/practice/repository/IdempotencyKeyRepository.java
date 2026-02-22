package com.practice.practice.repository;

import com.practice.practice.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    boolean existsByKey(String key);
}
