package com.riskscoring.enrichment.kafka;

import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.SignalsComputed;

public interface EnrichmentEventPublisher {

    void publishSignalsComputed(SignalsComputed event);

    void publishScanProgress(ScanProgress event);
}