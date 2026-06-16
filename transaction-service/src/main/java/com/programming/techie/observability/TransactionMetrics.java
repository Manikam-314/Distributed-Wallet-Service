package com.programming.techie.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Phase 7: Custom Prometheus Metrics for Transaction Service
 *
 * Metrics exposed:
 * - transaction_operations_total{operation=transfer} — counter
 * - transaction_operation_duration_seconds{operation=transfer} —
 * timer/histogram
 * - transaction_errors_total — error counter
 * - saga_operations_total{status=initiated|completed|failed} — saga tracking
 */
@Component
public class TransactionMetrics {

    private final Counter transferCounter;
    private final Counter transferErrorCounter;
    private final Counter sagaInitiatedCounter;
    private final Counter sagaCompletedCounter;
    private final Counter sagaFailedCounter;
    private final Timer transferTimer;

    public TransactionMetrics(MeterRegistry registry) {
        this.transferCounter = Counter.builder("transaction_operations_total")
                .tag("operation", "transfer")
                .description("Total transfer operations")
                .register(registry);

        this.transferErrorCounter = Counter.builder("transaction_errors_total")
                .description("Total transfer errors")
                .register(registry);

        this.sagaInitiatedCounter = Counter.builder("saga_operations_total")
                .tag("status", "initiated")
                .description("Total sagas initiated")
                .register(registry);

        this.sagaCompletedCounter = Counter.builder("saga_operations_total")
                .tag("status", "completed")
                .description("Total sagas completed")
                .register(registry);

        this.sagaFailedCounter = Counter.builder("saga_operations_total")
                .tag("status", "failed")
                .description("Total sagas failed")
                .register(registry);

        this.transferTimer = Timer.builder("transaction_operation_duration_seconds")
                .tag("operation", "transfer")
                .description("Transfer operation duration")
                .register(registry);
    }

    public void incrementTransfer() {
        transferCounter.increment();
    }

    public void incrementTransferError() {
        transferErrorCounter.increment();
    }

    public void incrementSagaInitiated() {
        sagaInitiatedCounter.increment();
    }

    public void incrementSagaCompleted() {
        sagaCompletedCounter.increment();
    }

    public void incrementSagaFailed() {
        sagaFailedCounter.increment();
    }

    public Timer getTransferTimer() {
        return transferTimer;
    }
}
