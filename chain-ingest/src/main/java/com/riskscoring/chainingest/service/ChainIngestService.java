package com.riskscoring.chainingest.service;

import com.riskscoring.common.event.ScanRequested;

public interface ChainIngestService {

    void ingest(ScanRequested event);
}