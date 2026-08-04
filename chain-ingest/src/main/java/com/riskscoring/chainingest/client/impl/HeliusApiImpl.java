package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HeliusApi;
import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.dto.helius.HeliusDisplayOptions;
import com.riskscoring.chainingest.client.dto.helius.HeliusParseRequest;
import com.riskscoring.chainingest.client.dto.helius.HeliusPortfolio;
import com.riskscoring.chainingest.client.dto.helius.HeliusPortfolioResponse;
import com.riskscoring.chainingest.client.dto.helius.HeliusRpcError;
import com.riskscoring.chainingest.client.dto.helius.HeliusRpcRequest;
import com.riskscoring.chainingest.client.dto.helius.HeliusSearchAssetsParams;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.exception.ChainDataException;
import com.riskscoring.chainingest.exception.ChainDataNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class HeliusApiImpl implements HeliusApi {

    private static final String PATH_ADDRESS_TRANSACTIONS = "/v0/addresses/%s/transactions";
    private static final String PATH_PARSE_TRANSACTIONS = "/v0/transactions";
    private static final String PATH_RPC = "/";

    private static final String PARAM_API_KEY = "api-key";
    private static final String PARAM_LIMIT = "limit";

    private static final String JSONRPC_VERSION = "2.0";
    private static final String RPC_ID = "risk-scoring";
    private static final String METHOD_SEARCH_ASSETS = "searchAssets";
    private static final String TOKEN_TYPE_FUNGIBLE = "fungible";
    private static final int ASSETS_PAGE_SIZE = 100;
    private static final int FIRST_PAGE = 1;

    private final HttpCallTemplate heliusCallTemplate;
    private final ChainIngestProperties properties;

    @Override
    public List<HeliusTransaction> addressTransactions(String address) {
        String path = PATH_ADDRESS_TRANSACTIONS.formatted(address);

        return List.of(heliusCallTemplate.get(path, authorized(builder -> builder
                .queryParam(PARAM_LIMIT, properties.helius().pageSize())), HeliusTransaction[].class));
    }

    @Override
    public HeliusTransaction transaction(String signature) {
        HeliusTransaction[] parsed = heliusCallTemplate.post(PATH_PARSE_TRANSACTIONS, authorized(),
                new HeliusParseRequest(List.of(signature)), HeliusTransaction[].class);

        return Arrays.stream(parsed).findFirst()
                .orElseThrow(() -> new ChainDataNotFoundException(
                        "Helius has no transaction %s".formatted(signature)));
    }

    @Override
    public HeliusPortfolio portfolio(String address) {
        HeliusPortfolioResponse response = heliusCallTemplate.post(PATH_RPC, authorized(),
                searchAssets(address), HeliusPortfolioResponse.class);

        Optional.ofNullable(response.error()).ifPresent(error -> {
            throw new ChainDataException("Helius rejected %s for %s: %s"
                    .formatted(METHOD_SEARCH_ASSETS, address, describe(error)));
        });

        return heliusCallTemplate.require(response.result(), PATH_RPC);
    }

    private HeliusRpcRequest searchAssets(String address) {
        return new HeliusRpcRequest(JSONRPC_VERSION, RPC_ID, METHOD_SEARCH_ASSETS,
                new HeliusSearchAssetsParams(address, TOKEN_TYPE_FUNGIBLE, ASSETS_PAGE_SIZE, FIRST_PAGE,
                        new HeliusDisplayOptions(true)));
    }

    private String describe(HeliusRpcError error) {
        return "%d %s".formatted(error.code(), error.message());
    }

    private Consumer<UriBuilder> authorized() {
        return builder -> builder.queryParam(PARAM_API_KEY, properties.helius().apiKey());
    }

    private Consumer<UriBuilder> authorized(Consumer<UriBuilder> parameters) {
        return authorized().andThen(parameters);
    }
}