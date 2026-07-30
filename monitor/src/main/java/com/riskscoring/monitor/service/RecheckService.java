package com.riskscoring.monitor.service;

import com.riskscoring.common.event.ScanCompleted;

public interface RecheckService {

    void triggerDueRechecks();

    void handleScanCompleted(ScanCompleted event);
}
