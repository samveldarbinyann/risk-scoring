package com.riskscoring.chainingest.service.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.ChainDataClientKey;
import com.riskscoring.chainingest.exception.UnsupportedChainException;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChainIngestServiceImpl implements ChainIngestService {

    private static final String FETCH_STARTED_MESSAGE = "Fetching on-chain data";
    private static final String FETCH_DONE_ADDRESS = "Found %d transfers across %d counterparties";
    private static final String FETCH_DONE_TRANSACTION = "Resolved transaction: %d participants, %d internal transfers";

    private final Map<ChainDataClientKey, ChainDataClient> chainDataClients;
    private final Map<ScanTarget, ChainFactsCacheService> chainFactsCaches;
    private final ChainEventPublisher chainEventPublisher;

    @Override
    public void ingest(ScanRequested event) {
        ChainDataClient client = resolveClient(event);
        publishProgress(event, FETCH_STARTED_MESSAGE);

        ChainFactsCacheService cache = chainFactsCaches.get(event.targetType());
        ChainFacts facts = cache.findFresh(event.target(), event.chain())
                .orElseGet(() -> fetchAndCache(event, client, cache));

        publishProgress(event, progressMessage(facts));

        chainEventPublisher.publishChainFetched(new ChainFetched(
                event.scanId(),
                event.targetType(),
                event.target(),
                event.chain(),
                facts,
                event.language(),
                Instant.now()
        ));
    }

    private ChainDataClient resolveClient(ScanRequested event) {
        ChainDataClientKey key = new ChainDataClientKey(event.chain().family(), event.targetType());

        return Optional.ofNullable(chainDataClients.get(key))
                .orElseThrow(() -> new UnsupportedChainException(event.chain(), event.targetType()));
    }

    private ChainFacts fetchAndCache(ScanRequested event, ChainDataClient client, ChainFactsCacheService cache) {
        ChainFacts facts = client.fetch(event.target(), event.chain());
        cache.store(event.target(), event.chain(), facts);

        return facts;
    }

    private String progressMessage(ChainFacts facts) {
        return switch (facts) {
            case AddressFacts address -> FETCH_DONE_ADDRESS.formatted(
                    address.snapshot().txCount(), address.counterparties().size());
            case TransactionFacts transaction -> FETCH_DONE_TRANSACTION.formatted(
                    transaction.transaction().parties().size(), transaction.transaction().nestedTransferCount());
        };
    }

    private void publishProgress(ScanRequested event, String message) {
        chainEventPublisher.publishScanProgress(
                new ScanProgress(event.scanId(), ScanStage.FETCHING, message, Instant.now()));
    }
}
