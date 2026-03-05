package com.programming.techie.monitoring;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class KafkaMetricsService {

    private final AtomicInteger totalEvents = new AtomicInteger();
    private final AtomicInteger successEvents = new AtomicInteger();
    private final AtomicInteger failedEvents = new AtomicInteger();
    private final AtomicInteger dlqEvents = new AtomicInteger();

    public void incrementTotal() {
        totalEvents.incrementAndGet();
    }

    public void incrementSuccess() {
        successEvents.incrementAndGet();
    }

    public void incrementFailure() {
        failedEvents.incrementAndGet();
    }

    public void incrementDLQ() {
        dlqEvents.incrementAndGet();
    }

    public void printMetrics() {
        System.out.println("📊 ===== KAFKA METRICS =====");
        System.out.println("Total Events: " + totalEvents.get());
        System.out.println("Success Events: " + successEvents.get());
        System.out.println("Failed Events: " + failedEvents.get());
        System.out.println("DLQ Events: " + dlqEvents.get());
        System.out.println("=============================");
    }
}
