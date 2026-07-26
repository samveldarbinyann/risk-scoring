package com.riskscoring.gateway.kafka;

import com.riskscoring.common.event.ScanRequested;

public interface ScanEventPublisher {

    void publishScanRequested(ScanRequested event);
}