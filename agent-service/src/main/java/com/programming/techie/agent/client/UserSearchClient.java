package com.programming.techie.agent.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSearchClient {

    private final WebClient webClient;

    public List<UserDto> getAllUsers(String bearerToken) {
        try {
            return webClient.get()
                    .uri("/api/auth/users")
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .bodyToFlux(UserDto.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("[UserSearchClient] Failed to fetch users: {}", e.getMessage());
            throw new RuntimeException("Unable to search users. Please verify connection.");
        }
    }

    public Long resolveUserByName(String name, String bearerToken) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        // Clean name parameter
        String targetName = name.toLowerCase().trim();

        // Check if numeric, representing raw User ID
        try {
            return Long.parseLong(targetName);
        } catch (NumberFormatException ignored) {}

        List<UserDto> users = getAllUsers(bearerToken);
        if (users == null || users.isEmpty()) {
            return null;
        }

        // Try exact match first
        for (UserDto user : users) {
            if (user.name() != null && user.name().toLowerCase().trim().equals(targetName)) {
                return user.id();
            }
        }

        // Try contains match
        for (UserDto user : users) {
            if (user.name() != null && user.name().toLowerCase().contains(targetName)) {
                return user.id();
            }
        }

        return null;
    }

    // Inner Record representing user details from auth-service
    public record UserDto(Long id, String name, String email, String mobileNumber) {}
}
