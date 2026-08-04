package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.TonApi;
import com.riskscoring.chainingest.mapper.TonTransactionSnapshotMapper;
import com.riskscoring.chainingest.mapper.TonValues;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TonTransactionDataClient implements ChainDataClient {

    private final TonApi tonApi;
    private final TonTransactionSnapshotMapper tonTransactionSnapshotMapper;
    private final TonValues values;

    @Override
    public ChainFamily family() {
        return ChainFamily.TON;
    }

    @Override
    public ScanTarget target() {
        return ScanTarget.TRANSACTION;
    }

    @Override
    public TransactionFacts fetch(String hash, Chain chain) {
        String target = values.normalize(hash);

        TransactionSnapshot snapshot = tonTransactionSnapshotMapper.fromTonApi(tonApi.event(target), target);

        log.info("Fetched transaction {} on {}: {} parties, {} token transfers (success={})",
                target, chain.displayName(), snapshot.parties().size(),
                snapshot.tokenTransferCount(), snapshot.success());

        return new TransactionFacts(snapshot);
    }
}
