package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.MoralisApi;
import com.riskscoring.chainingest.exception.UnsupportedChainException;
import com.riskscoring.chainingest.mapper.TransactionSnapshotMapper;
import com.riskscoring.common.model.EvmChain;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoralisTransactionDataClient implements ChainDataClient {

    private final MoralisApi moralisApi;
    private final TransactionSnapshotMapper transactionSnapshotMapper;

    @Override
    public ScanTarget target() {
        return ScanTarget.TRANSACTION;
    }

    @Override
    public TransactionFacts fetch(String hash, int chainId) {
        EvmChain chain = EvmChain.byId(chainId).orElseThrow(() -> new UnsupportedChainException(chainId));
        String target = hash.toLowerCase(Locale.ROOT);

        TransactionSnapshot snapshot = transactionSnapshotMapper.fromMoralis(moralisApi.transaction(target, chainId));

        log.info("Fetched transaction {} on {}: {} parties, {} internal, {} token transfers (success={})",
                target, chain.displayName(), snapshot.parties().size(),
                snapshot.internalTransferCount(), snapshot.erc20TransferCount(), snapshot.success());

        return new TransactionFacts(snapshot);
    }
}
