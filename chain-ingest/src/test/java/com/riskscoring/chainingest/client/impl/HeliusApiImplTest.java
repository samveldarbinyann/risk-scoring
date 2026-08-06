package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HttpCallTemplate;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class HeliusApiImplTest {

    @Mock
    private HttpCallTemplate heliusCallTemplate;

    @Mock
    private ChainIngestProperties properties;

    private HeliusApiImpl api;

    @BeforeEach
    void setUp() {
        api = new HeliusApiImpl(heliusCallTemplate, properties);
        lenient().when(properties.helius()).thenReturn(provider(25, "api-key-value"));
        lenient().when(heliusCallTemplate.require(any(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addressTransactionsAddsApiKeyAndLimitParamsAndFiltersNullEntries() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        HeliusTransaction tx = new HeliusTransaction("sig", 1L, "fee", null, null, null);
        when(heliusCallTemplate.get(eq("/v0/addresses/addr/transactions"), captor.capture(), eq(HeliusTransaction[].class)))
                .thenReturn(new HeliusTransaction[]{tx, null});

        List<HeliusTransaction> result = api.addressTransactions("addr");

        assertThat(result).containsExactly(tx);
        UriBuilder uriBuilder = mock(UriBuilder.class, RETURNS_SELF);
        captor.getValue().accept(uriBuilder);
        verify(uriBuilder).queryParam("api-key", "api-key-value");
        verify(uriBuilder).queryParam("limit", 25);
    }

    @Test
    void transactionPostsParseRequestWithSignature() {
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        HeliusTransaction tx = new HeliusTransaction("sig", 1L, "fee", null, null, null);
        when(heliusCallTemplate.post(eq("/v0/transactions"), any(Consumer.class), bodyCaptor.capture(), eq(HeliusTransaction[].class)))
                .thenReturn(new HeliusTransaction[]{tx});

        assertThat(api.transaction("sig")).isSameAs(tx);
        assertThat(bodyCaptor.getValue()).isEqualTo(new HeliusParseRequest(List.of("sig")));
    }

    @Test
    void transactionThrowsNotFoundWhenResultIsEmptyOrAllNull() {
        when(heliusCallTemplate.post(eq("/v0/transactions"), any(Consumer.class), any(), eq(HeliusTransaction[].class)))
                .thenReturn(new HeliusTransaction[]{null});

        assertThatThrownBy(() -> api.transaction("sig")).isInstanceOf(ChainDataNotFoundException.class);
    }

    @Test
    void portfolioBuildsSearchAssetsRpcRequest() {
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        HeliusPortfolio portfolio = new HeliusPortfolio(List.of(), null);
        when(heliusCallTemplate.post(eq("/"), any(Consumer.class), bodyCaptor.capture(), eq(HeliusPortfolioResponse.class)))
                .thenReturn(new HeliusPortfolioResponse(portfolio, null));

        assertThat(api.portfolio("addr")).isSameAs(portfolio);
        HeliusRpcRequest request = (HeliusRpcRequest) bodyCaptor.getValue();
        assertThat(request.method()).isEqualTo("searchAssets");
        assertThat(request.params()).isEqualTo(
                new HeliusSearchAssetsParams("addr", "fungible", 25, 1, new com.riskscoring.chainingest.client.dto.helius.HeliusDisplayOptions(true)));
    }

    @Test
    void portfolioThrowsChainDataExceptionWhenResponseHasError() {
        when(heliusCallTemplate.post(eq("/"), any(Consumer.class), any(), eq(HeliusPortfolioResponse.class)))
                .thenReturn(new HeliusPortfolioResponse(null, new HeliusRpcError(-1, "boom")));

        assertThatThrownBy(() -> api.portfolio("addr"))
                .isInstanceOf(ChainDataException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void portfolioThrowsChainDataExceptionWhenResultIsNull() {
        when(heliusCallTemplate.post(eq("/"), any(Consumer.class), any(), eq(HeliusPortfolioResponse.class)))
                .thenReturn(new HeliusPortfolioResponse(null, null));
        when(heliusCallTemplate.require(null, "/")).thenThrow(new ChainDataException("no payload"));

        assertThatThrownBy(() -> api.portfolio("addr")).isInstanceOf(ChainDataException.class);
    }

    private static ChainIngestProperties.Provider provider(int pageSize, String apiKey) {
        return new ChainIngestProperties.Provider(null, apiKey, pageSize, 0, 0, 0, null, null, null);
    }
}
