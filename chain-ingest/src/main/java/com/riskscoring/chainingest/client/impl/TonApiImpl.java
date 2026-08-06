package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.TonApi;
import com.riskscoring.chainingest.client.dto.tonapi.*;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class TonApiImpl implements TonApi {

    private static final String PATH_ACCOUNT = "/v2/accounts/%s";
    private static final String PATH_ACCOUNT_EVENTS = "/v2/accounts/%s/events";
    private static final String PATH_ACCOUNT_JETTONS = "/v2/accounts/%s/jettons";
    private static final String PATH_EVENT = "/v2/events/%s";

    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_SORT_ORDER = "sort_order";
    private static final String PARAM_CURRENCIES = "currencies";

    private static final String SORT_ASCENDING = "asc";
    private static final String CURRENCY_USD = "usd";
    private static final int SINGLE_EVENT = 1;

    private final HttpCallTemplate tonApiCallTemplate;
    private final ChainIngestProperties properties;

    @Override
    public TonAccount account(String address) {
        return tonApiCallTemplate.get(PATH_ACCOUNT.formatted(address), TonAccount.class);
    }

    @Override
    public List<TonEvent> accountEvents(String address) {
        TonEvents response = tonApiCallTemplate.get(PATH_ACCOUNT_EVENTS.formatted(address),
                limit(properties.tonApi().pageSize()), TonEvents.class);

        return rows(response.events());
    }

    @Override
    public Optional<TonEvent> firstEvent(String address) {
        TonEvents response = tonApiCallTemplate.get(PATH_ACCOUNT_EVENTS.formatted(address),
                limit(SINGLE_EVENT).andThen(builder -> builder.queryParam(PARAM_SORT_ORDER, SORT_ASCENDING)),
                TonEvents.class);

        return rows(response.events()).stream().findFirst();
    }

    @Override
    public List<TonJettonBalance> jettons(String address) {
        TonJettonsBalances response = tonApiCallTemplate.get(PATH_ACCOUNT_JETTONS.formatted(address),
                builder -> builder.queryParam(PARAM_CURRENCIES, CURRENCY_USD), TonJettonsBalances.class);

        return rows(response.balances());
    }

    @Override
    public TonEvent event(String hash) {
        return tonApiCallTemplate.get(PATH_EVENT.formatted(hash), TonEvent.class);
    }

    private Consumer<UriBuilder> limit(int limit) {
        return builder -> builder.queryParam(PARAM_LIMIT, limit);
    }

    private <T> List<T> rows(List<T> data) {
        return Objects.requireNonNullElse(data, List.of());
    }
}
