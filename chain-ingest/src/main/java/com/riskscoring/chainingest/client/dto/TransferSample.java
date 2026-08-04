package com.riskscoring.chainingest.client.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record TransferSample(List<Transfer> transfers, boolean truncated) {

    private static final Duration WINDOW_24H = Duration.ofHours(24);

    public int size() {
        return transfers.size();
    }

    public long txCount24h(Instant observedAt) {
        Instant windowStart = observedAt.minus(WINDOW_24H);
        return times()
                .filter(at -> at.isAfter(windowStart))
                .count();
    }

    public Instant lastActivityAt() {
        return times().max(Comparator.naturalOrder()).orElse(null);
    }

    private Stream<Instant> times() {
        return transfers.stream()
                .map(Transfer::at)
                .filter(Objects::nonNull);
    }
}
