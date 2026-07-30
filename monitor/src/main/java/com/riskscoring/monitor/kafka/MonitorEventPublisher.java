package com.riskscoring.monitor.kafka;

import com.riskscoring.common.event.AlertTriggered;
import com.riskscoring.common.event.ScanRequested;

public interface MonitorEventPublisher {

    void publishScanRequested(ScanRequested event);

    void publishAlertTriggered(AlertTriggered event);
}
