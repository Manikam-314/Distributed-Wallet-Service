package com.programming.techie.repository;
import com.programming.techie.model.UserEntity;
import  com.programming.techie.model.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    // find wallet by user
    Optional<WalletEntity> findByUser(UserEntity user);
}
