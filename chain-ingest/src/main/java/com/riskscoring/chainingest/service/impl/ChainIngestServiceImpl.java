package com.riskscoring.chainingest.service.impl;

import com.riskscoring.chainingest.client.ChainData;
import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.entity.AddressCache;
import com.riskscoring.chainingest.kafka.ChainEventPublisher;
import com.riskscoring.chainingest.mapper.AddressCacheMapper;
import com.riskscoring.chainingest.repository.AddressCacheRepository;
import com.riskscoring.chainingest.service.ChainIngestService;
import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChainIngestServiceImpl implements ChainIngestService {

    private final ChainDataClient chainDataClient;
    private final AddressCacheRepository addressCacheRepository;
    private final AddressCacheMapper addressCacheMapper;
    private final ChainEventPublisher chainEventPublisher;
    private final ChainIngestProperties properties;

    @Override
    @Transactional
    public void ingest(ScanRequested event) {
        publishProgress(event, ScanStage.FETCHING, "Fetching on-chain data");

        Optional<AddressCache> cached = addressCacheRepository
                .findByChainIdAndAddress(event.chainId(), event.address())
                .filter(this::isFresh);

        ChainData chainData = cached
                .map(this::fromCache)
                .orElseGet(() -> fetchAndCache(event));

        chainEventPublisher.publishChainFetched(new ChainFetched(
                event.scanId(),
                event.address(),
                event.chainId(),
                chainData.snapshot(),
                chainData.counterparties(),
                Instant.now()
        ));
    }

    private ChainData fromCache(AddressCache cache) {
        log.info("Cache hit for chainId={} address={}", cache.getChainId(), cache.getAddress());
        return new ChainData(
                addressCacheMapper.toSnapshot(cache),
                addressCacheMapper.toCounterparties(cache)
        );
    }

    private ChainData fetchAndCache(ScanRequested event) {
        ChainData chainData = chainDataClient.fetch(event.address(), event.chainId());
        Instant fetchedAt = Instant.now();

        AddressCache cache = addressCacheRepository
                .findByChainIdAndAddress(event.chainId(), event.address())
                .orElseGet(() -> addressCacheMapper.toEntity(
                        chainData.snapshot(), event.address(), event.chainId(), fetchedAt));

        addressCacheMapper.updateSnapshot(cache, chainData.snapshot(), fetchedAt);
        cache.replaceCounterparties(addressCacheMapper.toEntities(chainData.counterparties()));
        addressCacheRepository.save(cache);

        return chainData;
    }

    private boolean isFresh(AddressCache cache) {
        return cache.getFetchedAt().isAfter(Instant.now().minus(properties.cacheTtl()));
    }

    private void publishProgress(ScanRequested event, ScanStage stage, String message) {
        chainEventPublisher.publishScanProgress(
                new ScanProgress(event.scanId(), stage, message, Instant.now()));
    }
}