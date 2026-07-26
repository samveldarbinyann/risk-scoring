package com.riskscoring.chainingest.kafka.impl;

import com.riskscoring.chainingest.kafka.ChainEventPublisher;
import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.event.ScanProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChainEventPublisherImpl implements ChainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishChainFetched(ChainFetched event) {
        kafkaTemplate.send(Topics.CHAIN_FETCHED, event.scanId().toString(), event);
        log.info("Published {} scanId={} counterparties={}",
                Topics.CHAIN_FETCHED, event.scanId(), event.counterparties().size());
    }

    @Override
    public void publishScanProgress(ScanProgress event) {
        kafkaTemplate.send(Topics.SCAN_PROGRESS, event.scanId().toString(), event);
        log.debug("Published {} scanId={} stage={}", Topics.SCAN_PROGRESS, event.scanId(), event.stage());
    }
}