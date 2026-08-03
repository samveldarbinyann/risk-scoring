package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.MempoolApi;
import com.riskscoring.chainingest.client.dto.mempool.MempoolAddressStats;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.function.Consumer;

@Component
public class MempoolApiImpl implements MempoolApi {

    private static final Consumer<UriBuilder> NO_PARAMETERS = builder -> {
    };

    private static final String PATH_ADDRESS = "/address/%s";
    private static final String PATH_ADDRESS_TRANSACTIONS = "/address/%s/txs";
    private static final String PATH_TRANSACTION = "/tx/%s";

    private final HttpCallTemplate http;

    public MempoolApiImpl(@Qualifier("mempoolCallTemplate") HttpCallTemplate http) {
        this.http = http;
    }

    @Override
    public MempoolAddressStats addressStats(String address) {
        return http.get(PATH_ADDRESS.formatted(address), NO_PARAMETERS, MempoolAddressStats.class);
    }

    @Override
    public List<MempoolTransaction> addressTransactions(String address) {
        return List.of(http.get(
                PATH_ADDRESS_TRANSACTIONS.formatted(address), NO_PARAMETERS, MempoolTransaction[].class));
    }

    @Override
    public MempoolTransaction transaction(String txid) {
        return http.get(PATH_TRANSACTION.formatted(txid), NO_PARAMETERS, MempoolTransaction.class);
    }
}
