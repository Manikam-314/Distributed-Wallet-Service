package com.programming.techie.client;

import com.programming.techie.dto.UserDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);
    private final RestTemplate restTemplate;

    @Value("${auth.service.url:http://localhost:8093}")
    private String authServiceUrl;

    public AuthClient() {
        this.restTemplate = new RestTemplate();
    }

    @CircuitBreaker(name = "authService", fallbackMethod = "getUserFallback")
    @Retry(name = "authService")
    public UserDTO getUser(Long userId) {
        String url = authServiceUrl + "/auth/users/" + userId;
        return restTemplate.getForObject(url, UserDTO.class);
    }

    public UserDTO getUserFallback(Long userId, Throwable throwable) {
        log.error("Fallback triggered for getUser due to: {}", throwable.getMessage());
        return null;
    }
}
