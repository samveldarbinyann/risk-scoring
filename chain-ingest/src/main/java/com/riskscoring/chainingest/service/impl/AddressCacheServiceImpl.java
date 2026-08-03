package com.riskscoring.chainingest.service.impl;

import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.entity.AddressCache;
import com.riskscoring.chainingest.mapper.AddressCacheMapper;
import com.riskscoring.chainingest.repository.AddressCacheRepository;
import com.riskscoring.chainingest.service.ChainFactsCacheService;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.ChainFacts;
import com.riskscoring.common.model.ScanTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressCacheServiceImpl implements ChainFactsCacheService {

    private final AddressCacheRepository addressCacheRepository;
    private final AddressCacheMapper addressCacheMapper;
    private final ChainIngestProperties properties;

    @Override
    public ScanTarget target() {
        return ScanTarget.ADDRESS;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChainFacts> findFresh(String address, int chainId) {
        return addressCacheRepository.findByChainIdAndAddress(chainId, address)
                .filter(this::isFresh)
                .map(cache -> {
                    log.info("Cache hit for chainId={} address={}", chainId, address);
                    return addressCacheMapper.toFacts(cache);
                });
    }

    @Override
    @Transactional
    public void store(String address, int chainId, ChainFacts facts) {
        AddressFacts addressFacts = (AddressFacts) facts;

        AddressCache cache = addressCacheRepository.findByChainIdAndAddress(chainId, address)
                .orElseGet(() -> addressCacheMapper.newEntity(address, chainId));

        addressCacheMapper.updateSnapshot(cache, addressFacts.snapshot());
        cache.replaceCounterparties(addressCacheMapper.toEntities(addressFacts.counterparties()));

        addressCacheRepository.save(cache);
    }

    private boolean isFresh(AddressCache cache) {
        return cache.getFetchedAt().isAfter(Instant.now().minus(properties.cacheTtl()));
    }
}
