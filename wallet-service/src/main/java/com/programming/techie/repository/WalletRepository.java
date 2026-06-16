package com.programming.techie.repository;
//import com.programming.techie.entity.UserEntity;
import  com.programming.techie.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    // find wallet by user
    java.util.List<WalletEntity> findAllByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM WalletEntity w WHERE w.userId = :userId")
    java.util.Optional<WalletEntity> findFirstByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
