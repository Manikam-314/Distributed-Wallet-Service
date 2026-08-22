package com.programming.techie.agent.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClientConfig — configures the WebClient used by all tool clients.
 *
 * - Base URL is injected from ${gateway.base-url} (never hardcoded)
 * - Connect timeout: 3s — fail fast if gateway is unreachable
 * - Read timeout: 8s — enough for Saga to respond
 * - Logging filter: logs all outgoing requests and responses for observability
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${gateway.base-url}")
    private String gatewayBaseUrl;

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)   // 3s connect timeout
                .responseTimeout(Duration.ofSeconds(8))                 // 8s total response timeout
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(8, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl(gatewayBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("[AgentWebClient] → {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("[AgentWebClient] ← HTTP {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
