package com.programming.techie.repository;

import com.programming.techie.entity.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<InAppNotification, Long> {
    List<InAppNotification> findByEmailOrderByCreatedAtDesc(String email);
    List<InAppNotification> findByMobileNumberOrderByCreatedAtDesc(String mobileNumber);
}
