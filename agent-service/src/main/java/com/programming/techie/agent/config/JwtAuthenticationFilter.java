package com.programming.techie.agent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // 1. Gateway passes the Bearer token? The blueprint says "Authentication: Strict JWT SecurityContext parsing".
        // The API Gateway has StripPrefix=1 and AuthenticationFilter. 
        // If the token is forwarded by the Gateway (or if we hit the service directly):
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        Long userId = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            if (jwtUtils.validateToken(token)) {
                username = jwtUtils.extractUsername(token);
                userId = jwtUtils.extractUserId(token);
            }
        }

        // 2. Alternatively, API Gateway sends loggedInUserId header directly
        String gatewayUserId = request.getHeader("loggedInUserId");
        if (gatewayUserId != null && !gatewayUserId.isEmpty()) {
            userId = Long.parseLong(gatewayUserId);
            username = request.getHeader("loggedInUser");
        }

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Add ROLE_USER so we can use @PreAuthorize("hasRole('USER')") in tools
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
