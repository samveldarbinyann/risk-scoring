package com.riskscoring.paymentwatch.client;

import com.riskscoring.paymentwatch.exception.MoralisRateLimitException;
import com.riskscoring.paymentwatch.exception.MoralisRejectedException;
import com.riskscoring.paymentwatch.exception.PaymentWatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Slf4j
public class HttpCallTemplate {

    private static final Consumer<UriBuilder> NO_PARAMETERS = ignored -> {
    };

    private final String provider;
    private final RestClient restClient;
    private final RateLimiter rateLimiter;
    private final int rateLimitRetries;
    private final Duration rateLimitBackoff;

    public <T> T get(String path, Class<T> responseType) {
        return get(path, NO_PARAMETERS, responseType);
    }

    public <T> T get(String path, Consumer<UriBuilder> parameters, Class<T> responseType) {
        return call(path, () -> restClient.get()
                .uri(uri(path, parameters))
                .retrieve()
                .body(responseType));
    }

    public <T> T require(T payload, String path) {
        return Optional.ofNullable(payload)
                .orElseThrow(() -> new PaymentWatchException(
                        "%s returned no payload for path=%s".formatted(provider, path)));
    }

    private <T> T call(String path, Supplier<T> request) {
        for (int attempt = 1; ; attempt++) {
            try {
                return attempt(path, request);
            } catch (MoralisRateLimitException e) {
                if (attempt >= rateLimitRetries) {
                    throw e;
                }
                log.warn("Rate limited by {} on path={}, retrying ({}/{})", provider, path, attempt, rateLimitRetries);
                sleep(rateLimitBackoff);
            }
        }
    }

    private <T> T attempt(String path, Supplier<T> request) {
        rateLimiter.acquire();
        log.debug("{} call path={}", provider, path);

        T body;
        try {
            body = request.get();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new MoralisRateLimitException("%s rate limit hit for path=%s".formatted(provider, path));
        } catch (HttpClientErrorException e) {
            throw new MoralisRejectedException(
                    "%s rejected the request for path=%s: %s".formatted(provider, path, e.getStatusCode()));
        } catch (RestClientException e) {
            throw new PaymentWatchException("%s request failed for path=%s".formatted(provider, path), e);
        }

        return require(body, path);
    }

    private Function<UriBuilder, URI> uri(String path, Consumer<UriBuilder> parameters) {
        return builder -> {
            builder.path(path);
            parameters.accept(builder);
            return builder.build();
        };
    }

    private void sleep(Duration backoff) {
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentWatchException("Interrupted while backing off from a %s rate limit".formatted(provider), e);
        }
    }
}
