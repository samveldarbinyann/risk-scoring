package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.MempoolApi;
import com.riskscoring.chainingest.client.dto.mempool.MempoolAddressStats;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MempoolApiImpl implements MempoolApi {

    private static final String PATH_ADDRESS = "/address/%s";
    private static final String PATH_ADDRESS_TRANSACTIONS = "/address/%s/txs";
    private static final String PATH_TRANSACTION = "/tx/%s";

    private final HttpCallTemplate mempoolCallTemplate;

    @Override
    public MempoolAddressStats addressStats(String address) {
        return mempoolCallTemplate.get(PATH_ADDRESS.formatted(address), MempoolAddressStats.class);
    }

    @Override
    public List<MempoolTransaction> addressTransactions(String address) {
        return List.of(mempoolCallTemplate.get(
                PATH_ADDRESS_TRANSACTIONS.formatted(address), MempoolTransaction[].class));
    }

    @Override
    public MempoolTransaction transaction(String txid) {
        return mempoolCallTemplate.get(PATH_TRANSACTION.formatted(txid), MempoolTransaction.class);
    }
}
