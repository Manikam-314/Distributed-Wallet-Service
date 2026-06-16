package com.programming.techie.controller;

import com.programming.techie.entity.InAppNotification;
import com.programming.techie.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow frontend access
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/user")
    public ResponseEntity<List<InAppNotification>> getUserNotifications(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobileNumber) {
        
        if (email != null && !email.isEmpty()) {
            return ResponseEntity.ok(notificationRepository.findByEmailOrderByCreatedAtDesc(email));
        } else if (mobileNumber != null && !mobileNumber.isEmpty()) {
            return ResponseEntity.ok(notificationRepository.findByMobileNumberOrderByCreatedAtDesc(mobileNumber));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.ok().build();
    }
}
