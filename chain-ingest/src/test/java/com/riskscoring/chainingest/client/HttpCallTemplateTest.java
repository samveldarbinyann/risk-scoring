package com.riskscoring.chainingest.client;

import com.riskscoring.chainingest.exception.ChainDataException;
import com.riskscoring.chainingest.exception.ChainDataNotFoundException;
import com.riskscoring.chainingest.exception.ChainDataRateLimitException;
import com.riskscoring.chainingest.exception.ChainDataRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class HttpCallTemplateTest {

    private static final String PROVIDER = "test-provider";
    private static final String PATH = "/some/path";

    @Mock
    private RestClient restClient;

    @Mock
    private RateLimiter rateLimiter;

    private RestClient.RequestHeadersUriSpec getUriSpec;
    private RestClient.RequestHeadersSpec getHeadersSpec;
    private RestClient.RequestBodyUriSpec postUriSpec;
    private RestClient.RequestBodySpec postBodySpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        getHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        postUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        postBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
    }

    private HttpCallTemplate template(int retries) {
        return new HttpCallTemplate(PROVIDER, restClient, rateLimiter, retries, Duration.ofMillis(1));
    }

    private void stubGetChain() {
        when(restClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(any(Function.class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    private void stubPostChain() {
        when(restClient.post()).thenReturn(postUriSpec);
        when(postUriSpec.uri(any(Function.class))).thenReturn(postBodySpec);
        when(postBodySpec.body(any(Object.class))).thenReturn(postBodySpec);
        when(postBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void getReturnsBodyAndAcquiresRateLimiterOnce() {
        stubGetChain();
        when(responseSpec.body(String.class)).thenReturn("ok");

        String result = template(3).get(PATH, String.class);

        assertThat(result).isEqualTo("ok");
        verify(rateLimiter, times(1)).acquire();
    }

    @Test
    void postReturnsBodyAndAcquiresRateLimiterOnce() {
        stubPostChain();
        when(responseSpec.body(String.class)).thenReturn("ok");

        String result = template(3).post(PATH, "request-body", String.class);

        assertThat(result).isEqualTo("ok");
        verify(rateLimiter, times(1)).acquire();
    }

    @Test
    void requireThrowsChainDataExceptionOnNullPayload() {
        assertThatThrownBy(() -> template(3).require(null, PATH))
                .isInstanceOf(ChainDataException.class)
                .hasMessageContaining(PROVIDER)
                .hasMessageContaining(PATH);
    }

    @Test
    void getThrowsChainDataExceptionWhenBodyIsNull() {
        stubGetChain();
        when(responseSpec.body(String.class)).thenReturn(null);

        assertThatThrownBy(() -> template(3).get(PATH, String.class))
                .isInstanceOf(ChainDataException.class);
    }

    @Test
    void tooManyRequestsIsRetriedThenThrowsRateLimitExceptionWhenExhausted() {
        stubGetChain();
        when(responseSpec.body(String.class)).thenThrow(tooManyRequests());

        assertThatThrownBy(() -> template(2).get(PATH, String.class))
                .isInstanceOf(ChainDataRateLimitException.class);
        verify(rateLimiter, times(2)).acquire();
    }

    @Test
    void tooManyRequestsSucceedsOnRetry() {
        stubGetChain();
        when(responseSpec.body(String.class)).thenThrow(tooManyRequests()).thenReturn("ok");

        String result = template(3).get(PATH, String.class);

        assertThat(result).isEqualTo("ok");
        verify(rateLimiter, times(2)).acquire();
    }

    @Test
    void badRequestOrNotFoundMapsToChainDataNotFoundExceptionWithoutRetry() {
        stubGetChain();
        when(responseSpec.body(String.class)).thenThrow(clientError(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> template(3).get(PATH, String.class))
                .isInstanceOf(ChainDataNotFoundException.class);
        verify(rateLimiter, times(1)).acquire();
    }

    @Test
    void otherClientErrorMapsToChainDataRejectedException() {
        stubGetChain();
        when(responseSpec.body(String.class)).thenThrow(clientError(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> template(3).get(PATH, String.class))
                .isInstanceOf(ChainDataRejectedException.class);
    }

    @Test
    void genericRestClientExceptionMapsToChainDataException() {
        stubGetChain();
        when(responseSpec.body(String.class)).thenThrow(new ResourceAccessException("connection refused"));

        assertThatThrownBy(() -> template(3).get(PATH, String.class))
                .isInstanceOf(ChainDataException.class);
    }

    private static HttpClientErrorException tooManyRequests() {
        return (HttpClientErrorException) HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, new byte[0], null);
    }

    private static HttpClientErrorException clientError(HttpStatus status) {
        return (HttpClientErrorException) HttpClientErrorException.create(
                status, status.getReasonPhrase(), HttpHeaders.EMPTY, new byte[0], null);
    }
}
