package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.HeliusApi;
import com.riskscoring.chainingest.mapper.SolanaTransactionSnapshotMapper;
import com.riskscoring.chainingest.mapper.SolanaValues;
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
public class SolanaTransactionDataClient implements ChainDataClient {

    private final HeliusApi heliusApi;
    private final SolanaTransactionSnapshotMapper solanaTransactionSnapshotMapper;
    private final SolanaValues values;

    @Override
    public ChainFamily family() {
        return ChainFamily.SOLANA;
    }

    @Override
    public ScanTarget target() {
        return ScanTarget.TRANSACTION;
    }

    @Override
    public TransactionFacts fetch(String signature, Chain chain) {
        String target = values.normalize(signature);

        TransactionSnapshot snapshot = solanaTransactionSnapshotMapper.fromHelius(heliusApi.transaction(target));

        log.info("Fetched transaction {} on {}: {} parties, {} token transfers (success={})",
                target, chain.displayName(), snapshot.parties().size(),
                snapshot.tokenTransferCount(), snapshot.success());

        return new TransactionFacts(snapshot);
    }
}
