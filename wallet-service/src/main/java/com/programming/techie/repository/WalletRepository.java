package com.programming.techie.repository;
//import com.programming.techie.entity.UserEntity;
import  com.programming.techie.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    // find wallet by user
    Optional<WalletEntity> findByUserId(Long userId);
}
