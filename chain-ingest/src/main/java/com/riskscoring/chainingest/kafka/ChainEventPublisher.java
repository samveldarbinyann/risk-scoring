package com.riskscoring.chainingest.kafka;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.event.ScanProgress;

public interface ChainEventPublisher {

    void publishChainFetched(ChainFetched event);

    void publishScanProgress(ScanProgress event);
}