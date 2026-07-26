package com.riskscoring.chainingest.service.impl;

import com.riskscoring.chainingest.client.ChainData;
import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.kafka.ChainEventPublisher;
import com.riskscoring.chainingest.service.AddressCacheService;
import com.riskscoring.chainingest.service.ChainIngestService;
import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChainIngestServiceImpl implements ChainIngestService {

    private static final String FETCH_STARTED_MESSAGE = "Fetching on-chain data";
    private static final String FETCH_DONE_MESSAGE = "Found %d transfers across %d counterparties";

    private final ChainDataClient chainDataClient;
    private final AddressCacheService addressCacheService;
    private final ChainEventPublisher chainEventPublisher;

    @Override
    public void ingest(ScanRequested event) {
        publishProgress(event, FETCH_STARTED_MESSAGE);

        ChainData chainData = addressCacheService.findFresh(event.address(), event.chainId())
                .orElseGet(() -> fetchAndCache(event));

        publishProgress(event, FETCH_DONE_MESSAGE.formatted(
                chainData.snapshot().txCount(), chainData.counterparties().size()));

        chainEventPublisher.publishChainFetched(new ChainFetched(
                event.scanId(),
                event.address(),
                event.chainId(),
                chainData.snapshot(),
                chainData.counterparties(),
                Instant.now()
        ));
    }

    private ChainData fetchAndCache(ScanRequested event) {
        ChainData chainData = chainDataClient.fetch(event.address(), event.chainId());
        addressCacheService.store(event.address(), event.chainId(), chainData);

        return chainData;
    }

    private void publishProgress(ScanRequested event, String message) {
        chainEventPublisher.publishScanProgress(
                new ScanProgress(event.scanId(), ScanStage.FETCHING, message, Instant.now()));
    }
}
