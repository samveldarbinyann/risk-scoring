package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.TronGridApi;
import com.riskscoring.chainingest.client.dto.trongrid.*;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.exception.ChainDataNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class TronGridApiImpl implements TronGridApi {

    private static final String PATH_ACCOUNT = "/v1/accounts/%s";
    private static final String PATH_ACCOUNT_TRANSACTIONS = "/v1/accounts/%s/transactions";
    private static final String PATH_ACCOUNT_TRC20 = "/v1/accounts/%s/transactions/trc20";
    private static final String PATH_TRANSACTION = "/wallet/gettransactionbyid";
    private static final String PATH_TRANSACTION_INFO = "/wallet/gettransactioninfobyid";

    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_MIN_TIMESTAMP = "min_timestamp";
    private static final String PARAM_MAX_TIMESTAMP = "max_timestamp";

    private final HttpCallTemplate tronGridCallTemplate;
    private final ChainIngestProperties properties;

    @Override
    public Optional<TronAccount> account(String address) {
        TronAccountResponse response = tronGridCallTemplate.get(
                PATH_ACCOUNT.formatted(address), TronAccountResponse.class);

        return rows(response.data()).stream().findFirst();
    }

    @Override
    public List<TronTransaction> accountTransactions(String address) {
        TronTransactionsResponse response = tronGridCallTemplate.get(
                PATH_ACCOUNT_TRANSACTIONS.formatted(address), pageSize(), TronTransactionsResponse.class);

        return rows(response.data());
    }

    @Override
    public List<TronTrc20Transfer> accountTrc20Transfers(String address) {
        TronTrc20Response response = tronGridCallTemplate.get(
                PATH_ACCOUNT_TRC20.formatted(address), pageSize(), TronTrc20Response.class);

        return rows(response.data());
    }

    @Override
    public List<TronTrc20Transfer> trc20TransfersAt(String address, long blockTimestamp) {
        TronTrc20Response response = tronGridCallTemplate.get(PATH_ACCOUNT_TRC20.formatted(address),
                pageSize().andThen(builder -> builder
                        .queryParam(PARAM_MIN_TIMESTAMP, blockTimestamp)
                        .queryParam(PARAM_MAX_TIMESTAMP, blockTimestamp)),
                TronTrc20Response.class);

        return rows(response.data());
    }

    @Override
    public TronTransaction transaction(String txId) {
        TronTransaction transaction = tronGridCallTemplate.post(PATH_TRANSACTION,
                new TronTransactionRequest(txId, true), TronTransaction.class);

        if (transaction.rawData() == null) {
            throw new ChainDataNotFoundException("TronGrid has no transaction %s".formatted(txId));
        }

        return transaction;
    }

    @Override
    public TronTransactionInfo transactionInfo(String txId) {
        return tronGridCallTemplate.post(PATH_TRANSACTION_INFO,
                new TronTransactionRequest(txId, true), TronTransactionInfo.class);
    }

    private Consumer<UriBuilder> pageSize() {
        return builder -> builder.queryParam(PARAM_LIMIT, properties.tronGrid().pageSize());
    }

    private <T> List<T> rows(List<T> data) {
        return Objects.requireNonNullElse(data, List.of());
    }
}
