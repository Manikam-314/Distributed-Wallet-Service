package com.programming.techie.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Phase 7: Custom Prometheus Metrics for Auth Service
 *
 * Metrics exposed:
 * - auth_operations_total{operation=register|login} — counter
 * - auth_errors_total{operation=register|login} — error counter
 */
@Component
public class AuthMetrics {

    private final Counter registerCounter;
    private final Counter loginCounter;
    private final Counter registerErrorCounter;
    private final Counter loginErrorCounter;

    public AuthMetrics(MeterRegistry registry) {
        this.registerCounter = Counter.builder("auth_operations_total")
                .tag("operation", "register")
                .description("Total registration attempts")
                .register(registry);

        this.loginCounter = Counter.builder("auth_operations_total")
                .tag("operation", "login")
                .description("Total login attempts")
                .register(registry);

        this.registerErrorCounter = Counter.builder("auth_errors_total")
                .tag("operation", "register")
                .description("Total registration errors")
                .register(registry);

        this.loginErrorCounter = Counter.builder("auth_errors_total")
                .tag("operation", "login")
                .description("Total login errors")
                .register(registry);
    }

    public void incrementRegister() {
        registerCounter.increment();
    }

    public void incrementLogin() {
        loginCounter.increment();
    }

    public void incrementRegisterError() {
        registerErrorCounter.increment();
    }

    public void incrementLoginError() {
        loginErrorCounter.increment();
    }
}
