package com.programming.techie.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Phase 7: Custom Prometheus Metrics for Wallet Service
 *
 * Metrics exposed:
 * - wallet_operations_total{operation=create|deposit|debit|credit} — counter
 * - wallet_operation_duration_seconds{operation=...} — timer/histogram
 * - wallet_errors_total{operation=...} — error counter
 */
@Component
public class WalletMetrics {

    // Counters
    private final Counter walletCreateCounter;
    private final Counter walletDepositCounter;
    private final Counter walletDebitCounter;
    private final Counter walletCreditCounter;
    private final Counter walletErrorCounter;

    // Timers
    private final Timer depositTimer;
    private final Timer debitTimer;
    private final Timer creditTimer;

    public WalletMetrics(MeterRegistry registry) {

        this.walletCreateCounter = Counter.builder("wallet_operations_total")
                .tag("operation", "create")
                .description("Total wallet create operations")
                .register(registry);

        this.walletDepositCounter = Counter.builder("wallet_operations_total")
                .tag("operation", "deposit")
                .description("Total wallet deposit operations")
                .register(registry);

        this.walletDebitCounter = Counter.builder("wallet_operations_total")
                .tag("operation", "debit")
                .description("Total wallet debit operations")
                .register(registry);

        this.walletCreditCounter = Counter.builder("wallet_operations_total")
                .tag("operation", "credit")
                .description("Total wallet credit operations")
                .register(registry);

        this.walletErrorCounter = Counter.builder("wallet_errors_total")
                .description("Total wallet operation errors")
                .register(registry);

        this.depositTimer = Timer.builder("wallet_operation_duration_seconds")
                .tag("operation", "deposit")
                .description("Wallet deposit duration")
                .register(registry);

        this.debitTimer = Timer.builder("wallet_operation_duration_seconds")
                .tag("operation", "debit")
                .description("Wallet debit duration")
                .register(registry);

        this.creditTimer = Timer.builder("wallet_operation_duration_seconds")
                .tag("operation", "credit")
                .description("Wallet credit duration")
                .register(registry);
    }

    public void incrementCreate() {
        walletCreateCounter.increment();
    }

    public void incrementDeposit() {
        walletDepositCounter.increment();
    }

    public void incrementDebit() {
        walletDebitCounter.increment();
    }

    public void incrementCredit() {
        walletCreditCounter.increment();
    }

    public void incrementError() {
        walletErrorCounter.increment();
    }

    public Timer getDepositTimer() {
        return depositTimer;
    }

    public Timer getDebitTimer() {
        return debitTimer;
    }

    public Timer getCreditTimer() {
        return creditTimer;
    }
}
