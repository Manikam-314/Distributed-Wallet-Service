package com.programming.techie.filter;

import com.programming.techie.config.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationFilterGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationFilterGatewayFilterFactory.Config> {

    private final JwtUtils jwtUtils;

    public AuthenticationFilterGatewayFilterFactory(JwtUtils jwtUtils) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            if (org.springframework.http.HttpMethod.OPTIONS.equals(request.getMethod())) {
                return chain.filter(exchange);
            }

            final List<String> apiEndpoints = List.of("/auth/register", "/auth/login", "/auth/verify-otp");

            String path = request.getURI().getPath();
            boolean isApiSecured = apiEndpoints.stream()
                    .noneMatch(uri -> path.equals(uri) || path.equals("/api" + uri));

            if (isApiSecured) {
                if (!request.getHeaders().containsKey("Authorization")) {
                    return onError(exchange, "No Authorization Header", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().getOrEmpty("Authorization").get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                }

                if (jwtUtils.isInvalid(authHeader)) {
                    return onError(exchange, "Invalid Token", HttpStatus.UNAUTHORIZED);
                }

                Claims claims = jwtUtils.getClaims(authHeader);
                String userId = claims.get("userId", String.class);
                
                System.out.println("Gateway Filter: Extracted userId=" + userId + " for path=" + path);

                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("loggedInUser", claims.getSubject())
                        .header("loggedInUserId", userId != null ? userId : "")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    private ServerHttpRequest populateRequestWithHeaders(ServerWebExchange exchange, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        return exchange.getRequest().mutate()
                .header("loggedInUser", claims.getSubject())
                .header("loggedInUserId", userId != null ? String.valueOf(userId) : "")
                .build();
    }
}
