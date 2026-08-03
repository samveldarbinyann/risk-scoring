package com.riskscoring.chainingest.client;

import com.riskscoring.chainingest.exception.ChainDataException;
import com.riskscoring.chainingest.exception.ChainDataNotFoundException;
import com.riskscoring.chainingest.exception.ChainDataRateLimitException;
import com.riskscoring.chainingest.exception.ChainDataRejectedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Slf4j
public class HttpCallTemplate {

    private static final Set<Integer> NO_DATA_STATUSES =
            Set.of(HttpStatus.BAD_REQUEST.value(), HttpStatus.NOT_FOUND.value());

    private final String provider;
    private final RestClient restClient;
    private final RateLimiter rateLimiter;
    private final int rateLimitRetries;
    private final Duration rateLimitBackoff;

    public <T> T get(String path, Class<T> responseType) {
        return get(path, builder -> {
        }, responseType);
    }

    public <T> T get(String path, Consumer<UriBuilder> parameters, Class<T> responseType) {
        for (int attempt = 1; ; attempt++) {
            try {
                return attempt(path, parameters, responseType);
            } catch (ChainDataRateLimitException e) {
                if (attempt >= rateLimitRetries) {
                    throw e;
                }
                log.warn("Rate limited by {} on path={}, retrying ({}/{})", provider, path, attempt, rateLimitRetries);
                sleep(rateLimitBackoff);
            }
        }
    }

    public <T> T require(T payload, String path) {
        return Optional.ofNullable(payload)
                .orElseThrow(() -> new ChainDataException(
                        "%s returned no payload for path=%s".formatted(provider, path)));
    }

    private <T> T attempt(String path, Consumer<UriBuilder> parameters, Class<T> responseType) {
        rateLimiter.acquire();
        log.debug("{} call path={}", provider, path);

        T body;
        try {
            body = restClient.get()
                    .uri(builder -> {
                        builder.path(path);
                        parameters.accept(builder);
                        return builder.build();
                    })
                    .retrieve()
                    .body(responseType);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ChainDataRateLimitException("%s rate limit hit for path=%s".formatted(provider, path));
        } catch (HttpClientErrorException e) {
            throw rejected(path, e.getStatusCode());
        } catch (RestClientException e) {
            throw new ChainDataException("%s request failed for path=%s".formatted(provider, path), e);
        }

        return require(body, path);
    }

    private ChainDataRejectedException rejected(String path, HttpStatusCode status) {
        String message = "%s rejected the request for path=%s: %s".formatted(provider, path, status);
        return NO_DATA_STATUSES.contains(status.value())
                ? new ChainDataNotFoundException(message)
                : new ChainDataRejectedException(message);
    }

    private void sleep(Duration backoff) {
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChainDataException("Interrupted while backing off from a %s rate limit".formatted(provider), e);
        }
    }
}
