package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.EtherscanApi;
import com.riskscoring.chainingest.client.RateLimiter;
import com.riskscoring.chainingest.client.dto.EtherscanEnvelope;
import com.riskscoring.chainingest.client.dto.EtherscanTokenTx;
import com.riskscoring.chainingest.client.dto.EtherscanTx;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.exception.EtherscanException;
import com.riskscoring.chainingest.exception.EtherscanRateLimitException;
import com.riskscoring.chainingest.exception.EtherscanRejectedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class EtherscanApiImpl implements EtherscanApi {

    private static final String PARAM_CHAIN_ID = "chainid";
    private static final String PARAM_MODULE = "module";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ADDRESS = "address";
    private static final String PARAM_API_KEY = "apikey";
    private static final String PARAM_PAGE = "page";
    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_SORT = "sort";
    private static final String PARAM_TAG = "tag";

    private static final String MODULE_ACCOUNT = "account";
    private static final String ACTION_BALANCE = "balance";
    private static final String ACTION_TXLIST = "txlist";
    private static final String ACTION_TXLIST_INTERNAL = "txlistinternal";
    private static final String ACTION_TOKENTX = "tokentx";

    private static final String STATUS_OK = "1";
    private static final String SORT_ASC = "asc";
    private static final String SORT_DESC = "desc";
    private static final String TAG_LATEST = "latest";
    private static final int FIRST_PAGE = 1;
    private static final int SINGLE_RECORD = 1;

    private static final String RATE_LIMIT_MARKER = "rate limit";

    private final RestClient etherscanRestClient;
    private final ObjectMapper objectMapper;
    private final RateLimiter etherscanRateLimiter;
    private final ChainIngestProperties properties;

    @Override
    public String balanceWei(String address, int chainId) {
        JsonNode result = call(chainId, ACTION_BALANCE, builder -> builder
                .queryParam(PARAM_ADDRESS, address)
                .queryParam(PARAM_TAG, TAG_LATEST));

        return result.asString();
    }

    @Override
    public List<EtherscanTx> latestTransactions(String address, int chainId) {
        return toList(call(chainId, ACTION_TXLIST, latestPage(address)), EtherscanTx[].class);
    }

    @Override
    public List<EtherscanTx> latestInternalTransactions(String address, int chainId) {
        return toList(call(chainId, ACTION_TXLIST_INTERNAL, latestPage(address)), EtherscanTx[].class);
    }

    @Override
    public List<EtherscanTokenTx> latestTokenTransfers(String address, int chainId) {
        return toList(call(chainId, ACTION_TOKENTX, latestPage(address)), EtherscanTokenTx[].class);
    }

    @Override
    public Optional<EtherscanTx> firstTransaction(String address, int chainId) {
        JsonNode result = call(chainId, ACTION_TXLIST, builder -> builder
                .queryParam(PARAM_ADDRESS, address)
                .queryParam(PARAM_PAGE, FIRST_PAGE)
                .queryParam(PARAM_OFFSET, SINGLE_RECORD)
                .queryParam(PARAM_SORT, SORT_ASC));

        return toList(result, EtherscanTx[].class).stream().findFirst();
    }

    private Consumer<UriBuilder> latestPage(String address) {
        return builder -> builder
                .queryParam(PARAM_ADDRESS, address)
                .queryParam(PARAM_PAGE, FIRST_PAGE)
                .queryParam(PARAM_OFFSET, properties.etherscan().pageSize())
                .queryParam(PARAM_SORT, SORT_DESC);
    }

    private JsonNode call(int chainId, String action, Consumer<UriBuilder> parameters) {
        for (int attempt = 1; ; attempt++) {
            try {
                return attemptCall(chainId, action, parameters);
            } catch (EtherscanRateLimitException e) {
                if (attempt >= properties.etherscan().rateLimitRetries()) {
                    throw e;
                }
                log.warn("Rate limited on action={}, retrying ({}/{})",
                        action, attempt, properties.etherscan().rateLimitRetries());
                sleep(properties.etherscan().rateLimitBackoff());
            }
        }
    }

    private JsonNode attemptCall(int chainId, String action, Consumer<UriBuilder> parameters) {
        etherscanRateLimiter.acquire();
        log.debug("Etherscan call action={} chainId={}", action, chainId);

        try {
            EtherscanEnvelope envelope = etherscanRestClient.get()
                    .uri(builder -> {
                        builder.queryParam(PARAM_CHAIN_ID, chainId)
                                .queryParam(PARAM_MODULE, MODULE_ACCOUNT)
                                .queryParam(PARAM_ACTION, action)
                                .queryParam(PARAM_API_KEY, properties.etherscan().apiKey());
                        parameters.accept(builder);
                        return builder.build();
                    })
                    .retrieve()
                    .body(EtherscanEnvelope.class);

            return resultOf(envelope, action);
        } catch (RestClientException e) {
            throw new EtherscanException("Etherscan request failed for action=%s".formatted(action), e);
        }
    }

    private JsonNode resultOf(EtherscanEnvelope envelope, String action) {
        if (envelope == null || envelope.result() == null) {
            throw new EtherscanException("Etherscan returned an empty body for action=%s".formatted(action));
        }

        if (STATUS_OK.equals(envelope.status()) || envelope.result().isArray()) {
            return envelope.result();
        }

        String detail = envelope.result().asString();

        if (detail.toLowerCase(Locale.ROOT).contains(RATE_LIMIT_MARKER)) {
            throw new EtherscanRateLimitException(
                    "Etherscan rate limit hit for action=%s: %s".formatted(action, detail));
        }

        throw new EtherscanRejectedException(
                "Etherscan rejected the request for action=%s: %s".formatted(action, detail));
    }

    private void sleep(Duration backoff) {
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EtherscanException("Interrupted while backing off from an Etherscan rate limit", e);
        }
    }

    private <T> List<T> toList(JsonNode result, Class<T[]> arrayType) {
        return result.isArray()
                ? Arrays.asList(objectMapper.treeToValue(result, arrayType))
                : List.of();
    }
}
