package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.MempoolApi;
import com.riskscoring.chainingest.mapper.BitcoinTransactionSnapshotMapper;
import com.riskscoring.chainingest.mapper.BitcoinValues;
import com.riskscoring.common.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BitcoinTransactionDataClient implements ChainDataClient {

    private final MempoolApi mempoolApi;
    private final BitcoinTransactionSnapshotMapper bitcoinTransactionSnapshotMapper;
    private final BitcoinValues values;

    @Override
    public ChainFamily family() {
        return ChainFamily.BITCOIN;
    }

    @Override
    public ScanTarget target() {
        return ScanTarget.TRANSACTION;
    }

    @Override
    public TransactionFacts fetch(String hash, Chain chain) {
        String target = values.normalize(hash);

        TransactionSnapshot snapshot = bitcoinTransactionSnapshotMapper.fromMempool(mempoolApi.transaction(target));

        log.info("Fetched transaction {} on {}: {} parties (confirmed={})",
                target, chain.displayName(), snapshot.parties().size(), snapshot.success());

        return new TransactionFacts(snapshot);
    }
}
