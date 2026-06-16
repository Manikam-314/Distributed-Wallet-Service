package com.programming.techie.repository;

import com.programming.techie.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    Optional<OtpEntity> findTopByMobileNumberOrderByExpiryTimeDesc(String mobileNumber);
}
