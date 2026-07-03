package com.example.ordercancel.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Micrometer Metrics
 * -------------------------
 * Registers application-level metrics visible at /actuator/prometheus
 * and scrapable by Prometheus → Grafana.
 */
@Configuration
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    // Cache counters by tag to avoid repeated lookup
    private final Map<String, Counter> counterCache = new ConcurrentHashMap<>();

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementOrderCreated(String approach) {
        getOrCreateCounter("orders.created", approach).increment();
    }

    public void incrementOrderCancelled(String approach, String reason) {
        getOrCreateTaggedCounter("orders.cancelled",
                Tags.of("approach", approach, "reason", sanitize(reason))).increment();
    }

    public void incrementOrderPaid(String approach) {
        getOrCreateCounter("orders.paid", approach).increment();
    }

    private Counter getOrCreateCounter(String name, String approach) {
        String key = name + "." + approach;
        return counterCache.computeIfAbsent(key, k ->
                Counter.builder(name)
                        .description("Number of orders with event: " + name)
                        .tag("approach", approach)
                        .register(meterRegistry)
        );
    }

    private Counter getOrCreateTaggedCounter(String name, Tags tags) {
        String key = name + tags.toString();
        return counterCache.computeIfAbsent(key, k ->
                Counter.builder(name)
                        .tags(tags)
                        .register(meterRegistry)
        );
    }

    /** Sanitize a free-form reason string to be a safe Prometheus label value */
    private String sanitize(String input) {
        if (input == null) return "unknown";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .substring(0, Math.min(input.length(), 50));
    }
}
