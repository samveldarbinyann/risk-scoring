package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.MoralisApi;
import com.riskscoring.chainingest.mapper.MoralisValues;
import com.riskscoring.chainingest.mapper.TransactionSnapshotMapper;
import com.riskscoring.common.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class MoralisTransactionDataClient implements ChainDataClient {

    private final MoralisApi moralisApi;
    private final MoralisValues values;
    private final TransactionSnapshotMapper transactionSnapshotMapper;

    @Override
    public ChainFamily family() {
        return ChainFamily.EVM;
    }

    @Override
    public ScanTarget target() {
        return ScanTarget.TRANSACTION;
    }

    @Override
    public TransactionFacts fetch(String hash, Chain chain) {
        String target = values.address(hash);

        TransactionSnapshot snapshot = transactionSnapshotMapper.fromMoralis(moralisApi.transaction(target, chain));

        log.info("Fetched transaction {} on {}: {} parties, {} internal, {} token transfers (success={})",
                target, chain.displayName(), snapshot.parties().size(),
                snapshot.nestedTransferCount(), snapshot.tokenTransferCount(), snapshot.success());

        return new TransactionFacts(snapshot);
    }
}
