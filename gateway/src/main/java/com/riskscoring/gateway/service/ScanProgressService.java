package com.riskscoring.gateway.service;

import com.riskscoring.common.event.ScanProgress;

public interface ScanProgressService {

    void applyProgress(ScanProgress event);
}