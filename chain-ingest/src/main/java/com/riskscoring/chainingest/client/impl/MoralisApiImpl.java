package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.MoralisApi;
import com.riskscoring.chainingest.client.RateLimiter;
import com.riskscoring.chainingest.client.dto.MoralisActiveChain;
import com.riskscoring.chainingest.client.dto.MoralisBalance;
import com.riskscoring.chainingest.client.dto.MoralisHistoryEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalance;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalancesEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisWalletChainsEnvelope;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.exception.MoralisException;
import com.riskscoring.chainingest.exception.MoralisNoDataException;
import com.riskscoring.chainingest.exception.MoralisRateLimitException;
import com.riskscoring.chainingest.exception.MoralisRejectedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoralisApiImpl implements MoralisApi {

    private static final String PARAM_CHAIN = "chain";
    private static final String PARAM_CHAINS = "chains";
    private static final String PARAM_ORDER = "order";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_INCLUDE_INTERNAL = "include_internal_transactions";
    private static final String PARAM_EXCLUDE_SPAM = "exclude_spam";
    private static final String PARAM_EXCLUDE_NATIVE = "exclude_native";
    private static final String ORDER_DESC = "DESC";

    private static final String PATH_BALANCE = "/%s/balance";
    private static final String PATH_HISTORY = "/wallets/%s/history";
    private static final String PATH_CHAINS = "/wallets/%s/chains";
    private static final String PATH_TOKENS = "/wallets/%s/tokens";

    private static final Set<Integer> NO_DATA_STATUSES =
            Set.of(HttpStatus.BAD_REQUEST.value(), HttpStatus.NOT_FOUND.value());

    private final RestClient moralisRestClient;
    private final RateLimiter moralisRateLimiter;
    private final ChainIngestProperties properties;

    @Override
    public String balanceWei(String address, int chainId) {
        String path = PATH_BALANCE.formatted(address);
        MoralisBalance balance = call(path, builder -> builder
                .queryParam(PARAM_CHAIN, chainHex(chainId)), MoralisBalance.class);

        return requirePayload(balance.balance(), path);
    }

    @Override
    public MoralisHistoryEnvelope walletHistory(String address, int chainId) {
        String path = PATH_HISTORY.formatted(address);
        MoralisHistoryEnvelope envelope = call(path, builder -> builder
                .queryParam(PARAM_CHAIN, chainHex(chainId))
                .queryParam(PARAM_ORDER, ORDER_DESC)
                .queryParam(PARAM_LIMIT, properties.moralis().pageSize())
                .queryParam(PARAM_INCLUDE_INTERNAL, true), MoralisHistoryEnvelope.class);

        requirePayload(envelope.result(), path);
        return envelope;
    }

    @Override
    public Optional<MoralisActiveChain> walletActivity(String address, int chainId) {
        String path = PATH_CHAINS.formatted(address);

        try {
            List<MoralisActiveChain> activeChains = call(path, builder -> builder
                    .queryParam(PARAM_CHAINS, chainHex(chainId)), MoralisWalletChainsEnvelope.class)
                    .activeChains();

            return activeChains == null ? Optional.empty() : activeChains.stream().findFirst();
        } catch (MoralisNoDataException e) {
            log.debug("No wallet-activity data for address={} chainId={}: {}", address, chainId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<MoralisTokenBalance> tokenBalances(String address, int chainId) {
        String path = PATH_TOKENS.formatted(address);
        MoralisTokenBalancesEnvelope envelope = call(path, builder -> builder
                .queryParam(PARAM_CHAIN, chainHex(chainId))
                .queryParam(PARAM_EXCLUDE_SPAM, true)
                .queryParam(PARAM_EXCLUDE_NATIVE, true), MoralisTokenBalancesEnvelope.class);

        return requirePayload(envelope.result(), path);
    }

    private String chainHex(int chainId) {
        return "0x" + Integer.toHexString(chainId);
    }

    private <T> T requirePayload(T payload, String path) {
        return Optional.ofNullable(payload)
                .orElseThrow(() -> new MoralisException("Moralis returned no payload for path=%s".formatted(path)));
    }

    private <T> T call(String path, Consumer<UriBuilder> parameters, Class<T> responseType) {
        for (int attempt = 1; ; attempt++) {
            try {
                return attemptCall(path, parameters, responseType);
            } catch (MoralisRateLimitException e) {
                if (attempt >= properties.moralis().rateLimitRetries()) {
                    throw e;
                }
                log.warn("Rate limited on path={}, retrying ({}/{})",
                        path, attempt, properties.moralis().rateLimitRetries());
                sleep(properties.moralis().rateLimitBackoff());
            }
        }
    }

    private <T> T attemptCall(String path, Consumer<UriBuilder> parameters, Class<T> responseType) {
        moralisRateLimiter.acquire();
        log.debug("Moralis call path={}", path);

        T body;
        try {
            body = moralisRestClient.get()
                    .uri(builder -> {
                        builder.path(path);
                        parameters.accept(builder);
                        return builder.build();
                    })
                    .retrieve()
                    .body(responseType);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new MoralisRateLimitException("Moralis rate limit hit for path=%s".formatted(path));
        } catch (HttpClientErrorException e) {
            throw rejected(path, e.getStatusCode());
        } catch (RestClientException e) {
            throw new MoralisException("Moralis request failed for path=%s".formatted(path), e);
        }

        return requirePayload(body, path);
    }

    private MoralisRejectedException rejected(String path, HttpStatusCode status) {
        String message = "Moralis rejected the request for path=%s: %s".formatted(path, status);
        return NO_DATA_STATUSES.contains(status.value())
                ? new MoralisNoDataException(message)
                : new MoralisRejectedException(message);
    }

    private void sleep(Duration backoff) {
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MoralisException("Interrupted while backing off from a Moralis rate limit", e);
        }
    }
}
