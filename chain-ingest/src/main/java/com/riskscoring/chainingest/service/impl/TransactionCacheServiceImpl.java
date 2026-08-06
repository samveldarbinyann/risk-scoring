package com.riskscoring.chainingest.service.impl;

import com.riskscoring.chainingest.mapper.TransactionCacheMapper;
import com.riskscoring.chainingest.repository.TransactionCacheRepository;
import com.riskscoring.chainingest.service.ChainFactsCacheService;
import com.riskscoring.common.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionCacheServiceImpl implements ChainFactsCacheService {

    private final TransactionCacheRepository transactionCacheRepository;
    private final TransactionCacheMapper transactionCacheMapper;

    @Override
    public ScanTarget target() {
        return ScanTarget.TRANSACTION;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChainFacts> findFresh(String txHash, Chain chain) {
        return transactionCacheRepository.findByChainAndTxHash(chain, txHash)
                .map(cache -> {
                    log.info("Cache hit for chain={} txHash={}", chain, txHash);
                    return new TransactionFacts(transactionCacheMapper.toSnapshot(cache));
                });
    }

    @Override
    @Transactional
    public void store(String txHash, Chain chain, ChainFacts facts) {
        TransactionSnapshot snapshot = ((TransactionFacts) facts).transaction();

        if (snapshot.blockTimestamp() == null) {
            log.info("Skipping cache for pending transaction chain={} txHash={}", chain, txHash);
            return;
        }

        transactionCacheRepository.findByChainAndTxHash(chain, txHash)
                .ifPresentOrElse(
                        existing -> log.debug("Transaction already cached chain={} txHash={}", chain, txHash),
                        () -> transactionCacheRepository.save(transactionCacheMapper.toEntity(chain, snapshot)));
    }
}
