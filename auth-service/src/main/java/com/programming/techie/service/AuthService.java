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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final WebClient webClient;   // ⭐ add this

    // wallet service base url
    private final String WALLET_SERVICE_URL = "http://localhost:8081";

    public void register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        // create user
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        UserEntity savedUser = userRepository.save(user);

        // ⭐ CALL WALLET SERVICE TO CREATE WALLET
        CreateWalletRequest walletRequest = new CreateWalletRequest();
        walletRequest.setUserId(savedUser.getId());
try{
        webClient.post()
                .uri(WALLET_SERVICE_URL + "/wallet/create")
                .bodyValue(walletRequest)
              .retrieve()
                .bodyToMono(String.class)
                .block();   // wait for response
    }
     catch(Exception e) {
        userRepository.delete(savedUser);
        throw new RuntimeException("Wallet creation failed");
    }
}
    public AuthResponse login(LoginRequest request) {

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(token);
    }
}