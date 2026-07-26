package com.riskscoring.riskai.kafka;

import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.common.event.ScanProgress;

public interface RiskAiEventPublisher {

    void publishScanCompleted(ScanCompleted event);

    void publishScanProgress(ScanProgress event);
}
