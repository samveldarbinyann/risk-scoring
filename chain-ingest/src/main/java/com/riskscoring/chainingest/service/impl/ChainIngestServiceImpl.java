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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChainIngestServiceImpl implements ChainIngestService {

    private static final String FETCH_STARTED_KEY = "console.message.fetchStarted";
    private static final String FETCH_DONE_ADDRESS_KEY = "console.message.fetchDoneAddress";
    private static final String FETCH_DONE_TRANSACTION_KEY = "console.message.fetchDoneTransaction";

    private final Map<ChainDataClientKey, ChainDataClient> chainDataClients;
    private final Map<ScanTarget, ChainFactsCacheService> chainFactsCaches;
    private final ChainEventPublisher chainEventPublisher;

    @Override
    public void ingest(ScanRequested event) {
        ChainDataClient client = resolveClient(event);
        publishProgress(event, FETCH_STARTED_KEY, List.of());

        ChainFactsCacheService cache = chainFactsCaches.get(event.targetType());
        ChainFacts facts = cache.findFresh(event.target(), event.chain())
                .orElseGet(() -> fetchAndCache(event, client, cache));

        publishProgress(event, progressMessageKey(facts), progressMessageArgs(facts));

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

    private String progressMessageKey(ChainFacts facts) {
        return switch (facts) {
            case AddressFacts address -> FETCH_DONE_ADDRESS_KEY;
            case TransactionFacts transaction -> FETCH_DONE_TRANSACTION_KEY;
        };
    }

    private List<Object> progressMessageArgs(ChainFacts facts) {
        return switch (facts) {
            case AddressFacts address -> List.of(address.snapshot().txCount(), address.counterparties().size());
            case TransactionFacts transaction ->
                    List.of(transaction.transaction().parties().size(), transaction.transaction().nestedTransferCount());
        };
    }

    private void publishProgress(ScanRequested event, String messageKey, List<Object> args) {
        chainEventPublisher.publishScanProgress(
                new ScanProgress(event.scanId(), ScanStage.FETCHING, messageKey, args, event.language(), Instant.now()));
    }
}
