package com.riskscoring.monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "monitor")
public record MonitorProperties(
        Duration recheckInterval,
        Duration schedulerFixedDelay,
        int batchSize,
        Duration stalePendingTimeout
) {
}
