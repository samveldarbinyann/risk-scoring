package com.riskscoring.enrichment.kafka;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.enrichment.service.EnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChainFetchedListener {

    private final EnrichmentService enrichmentService;

    @KafkaListener(topics = Topics.CHAIN_FETCHED)
    public void onChainFetched(ChainFetched event) {
        log.info("Received {} scanId={} address={}", Topics.CHAIN_FETCHED, event.scanId(), event.address());
        enrichmentService.enrich(event);
    }
}