package com.practice.practice.repository;
import com.practice.practice.model.UserEntity;
import  com.practice.practice.model.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    // find wallet by user
    Optional<WalletEntity> findByUser(UserEntity user);
}
