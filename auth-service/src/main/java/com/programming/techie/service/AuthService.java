package com.programming.techie.service;

import com.programming.techie.dto.*;
import com.programming.techie.entity.UserEntity;
import com.programming.techie.repository.UserRepository;
import com.programming.techie.exception.ResourceNotFoundException;
import com.programming.techie.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.programming.techie.dto.LoginRequest;
import com.programming.techie.dto.RegisterRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final WebClient webClient;
    private final OtpService otpService;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    // wallet service base url
    private final String WALLET_SERVICE_URL = "http://localhost:8091";

    public void register(RegisterRequest request) {
        java.util.Optional<UserEntity> existingUser = userRepository.findByEmail(request.getEmail());
        
        UserEntity user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.isVerified()) {
                throw new RuntimeException("User already exists and is verified");
            }
            log.info("Updating existing unverified user: {}", request.getEmail());
        } else {
            user = new UserEntity();
        }

        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMobileNumber(request.getMobileNumber());
        user.setRole("USER");
        user.setVerified(false); // Ensure it's false initially

        UserEntity savedUser = userRepository.save(user);

        // ⭐ EMIT USER REGISTERED EVENT VERIFIED TO KAFKA
        com.programming.techie.events.UserRegisteredEvent userEvent = com.programming.techie.events.UserRegisteredEvent.builder()
                .userId(savedUser.getId())
                .timestamp(java.time.Instant.now())
                .build();
        kafkaTemplate.send("user-registered-topic", savedUser.getId().toString(), userEvent);
        log.info("Emitted UserRegisteredEvent for userId: {}", savedUser.getId());

        // Generate and Send OTP via BOTH channels initially
        String otp = otpService.generateOtp(savedUser.getMobileNumber());
        com.programming.techie.events.NotificationEvent event = com.programming.techie.events.NotificationEvent.builder()
                .mobileNumber(savedUser.getMobileNumber())
                .email(savedUser.getEmail())
                .message("Welcome to Distributed Wallet! Your OTP for registration is: " + otp)
                .deliveryChannel("BOTH")
                .build();
        kafkaTemplate.send("notificationTopic", event);
    }

    // Wallet service is now event-driven. Retries are managed automatically by Kafka DLQ configurations.


    public AuthResponse login(LoginRequest request) {

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.programming.techie.exception.BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new com.programming.techie.exception.BadCredentialsException("Invalid credentials");
        }

        // User requested OTP to not be required for login phase.
        // if (!user.isVerified()) {
        //     throw new RuntimeException("Account not verified. Please verify your OTP.");
        // }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }

    public java.util.List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getMobileNumber(), user.isVerified()))
                .collect(java.util.stream.Collectors.toList());
    }

    public boolean verifyOtp(String mobileNumber, String otp) {
        boolean isVerified = otpService.verifyOtp(mobileNumber, otp);
        if (isVerified) {
            userRepository.findAllByMobileNumber(mobileNumber).stream()
                .filter(u -> !u.isVerified())
                .forEach(user -> {
                    user.setVerified(true);
                    userRepository.save(user);
                    log.info("User {} ({}) verified successfully via OTP", mobileNumber, user.getEmail());
                });
        }
        return isVerified;
    }

    public void resendOtp(String email, String channel) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // Generate new OTP
        String otp = otpService.generateOtp(user.getMobileNumber());

        com.programming.techie.events.NotificationEvent event = com.programming.techie.events.NotificationEvent.builder()
                .mobileNumber(user.getMobileNumber())
                .email(user.getEmail())
                .message("Your new OTP for Distributed Wallet is: " + otp)
                .deliveryChannel(channel)
                .build();

        kafkaTemplate.send("notificationTopic", event);
        log.info("Resent OTP to {} via {}", email, channel);
    }

    public UserDTO getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getMobileNumber(), user.isVerified()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
