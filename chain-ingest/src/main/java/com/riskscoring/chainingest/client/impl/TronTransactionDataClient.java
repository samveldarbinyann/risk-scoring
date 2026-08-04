package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.TronGridApi;
import com.riskscoring.chainingest.client.dto.trongrid.TronContractValue;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransactionInfo;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import com.riskscoring.chainingest.mapper.TronTransactionSnapshotMapper;
import com.riskscoring.chainingest.mapper.TronValues;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TronTransactionDataClient implements ChainDataClient {

    private static final long NO_BLOCK = 0L;

    private final TronGridApi tronGridApi;
    private final TronTransactionSnapshotMapper tronTransactionSnapshotMapper;
    private final TronValues values;

    @Override
    public ChainFamily family() {
        return ChainFamily.TRON;
    }

    @Override
    public ScanTarget target() {
        return ScanTarget.TRANSACTION;
    }

    @Override
    public TransactionFacts fetch(String hash, Chain chain) {
        String target = values.normalize(hash);

        TronTransaction transaction = tronGridApi.transaction(target);
        TronTransactionInfo info = tronGridApi.transactionInfo(target);

        TransactionSnapshot snapshot = tronTransactionSnapshotMapper.fromTronGrid(
                transaction, info, tokenTransfers(transaction, info, target));

        log.info("Fetched transaction {} on {}: {} parties, {} token transfers (success={})",
                target, chain.displayName(), snapshot.parties().size(),
                snapshot.tokenTransferCount(), snapshot.success());

        return new TransactionFacts(snapshot);
    }

    private List<TronTrc20Transfer> tokenTransfers(TronTransaction transaction,
                                                   TronTransactionInfo info,
                                                   String txId) {
        if (info.blockTimeStamp() == NO_BLOCK) {
            return List.of();
        }

        return values.contract(transaction)
                .filter(contract -> TronValues.TRIGGER_SMART_CONTRACT.equals(contract.type()))
                .flatMap(values::value)
                .map(TronContractValue::ownerAddress)
                .map(values::address)
                .filter(values::isRoutable)
                .map(owner -> tronGridApi.trc20TransfersAt(owner, info.blockTimeStamp()).stream()
                        .filter(transfer -> txId.equalsIgnoreCase(transfer.transactionId()))
                        .toList())
                .orElseGet(List::of);
    }
}
