package com.riskscoring.chainingest.service.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.kafka.ChainEventPublisher;
import com.riskscoring.chainingest.service.ChainFactsCacheService;
import com.riskscoring.chainingest.service.ChainIngestService;
import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.ChainFacts;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TransactionFacts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChainIngestServiceImpl implements ChainIngestService {

    private static final String FETCH_STARTED_MESSAGE = "Fetching on-chain data";
    private static final String FETCH_DONE_ADDRESS = "Found %d transfers across %d counterparties";
    private static final String FETCH_DONE_TRANSACTION = "Resolved transaction: %d participants, %d internal transfers";

    private final Map<ScanTarget, ChainDataClient> chainDataClients;
    private final Map<ScanTarget, ChainFactsCacheService> chainFactsCaches;
    private final ChainEventPublisher chainEventPublisher;

    @Override
    public void ingest(ScanRequested event) {
        publishProgress(event, FETCH_STARTED_MESSAGE);

        ChainFactsCacheService cache = chainFactsCaches.get(event.targetType());
        ChainFacts facts = cache.findFresh(event.target(), event.chainId())
                .orElseGet(() -> fetchAndCache(event, cache));

        publishProgress(event, progressMessage(facts));

        chainEventPublisher.publishChainFetched(new ChainFetched(
                event.scanId(),
                event.targetType(),
                event.target(),
                event.chainId(),
                facts,
                event.language(),
                Instant.now()
        ));
    }

    private ChainFacts fetchAndCache(ScanRequested event, ChainFactsCacheService cache) {
        ChainFacts facts = chainDataClients.get(event.targetType()).fetch(event.target(), event.chainId());
        cache.store(event.target(), event.chainId(), facts);

        return facts;
    }

    private String progressMessage(ChainFacts facts) {
        return switch (facts) {
            case AddressFacts address -> FETCH_DONE_ADDRESS.formatted(
                    address.snapshot().txCount(), address.counterparties().size());
            case TransactionFacts transaction -> FETCH_DONE_TRANSACTION.formatted(
                    transaction.transaction().parties().size(), transaction.transaction().internalTransferCount());
        };
    }

    private void publishProgress(ScanRequested event, String message) {
        chainEventPublisher.publishScanProgress(
                new ScanProgress(event.scanId(), ScanStage.FETCHING, message, Instant.now()));
    }
}
