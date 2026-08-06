package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.dto.MoralisActiveChain;
import com.riskscoring.chainingest.client.dto.MoralisBalance;
import com.riskscoring.chainingest.client.dto.MoralisHistoryEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalance;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalancesEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.chainingest.client.dto.MoralisWalletChainsEnvelope;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.exception.ChainDataException;
import com.riskscoring.chainingest.exception.ChainDataNotFoundException;
import com.riskscoring.common.model.Chain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class MoralisApiImplTest {

    @Mock
    private HttpCallTemplate moralisCallTemplate;

    @Mock
    private ChainIngestProperties properties;

    private MoralisApiImpl api;

    @BeforeEach
    void setUp() {
        api = new MoralisApiImpl(moralisCallTemplate, properties);
        lenient().when(properties.moralis()).thenReturn(provider(25));
        lenient().when(moralisCallTemplate.require(any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void balanceNativeBuildsPathAndChainQueryParamAndUnwrapsViaRequire() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(moralisCallTemplate.get(eq("/0xabc/balance"), captor.capture(), eq(MoralisBalance.class)))
                .thenReturn(new MoralisBalance("12345"));

        String result = api.balanceNative("0xabc", Chain.ETHEREUM);

        assertThat(result).isEqualTo("12345");
        UriBuilder uriBuilder = captureAndApply(captor.getValue());
        verify(uriBuilder).queryParam("chain", "0x1");
    }

    @Test
    void transactionAddsChainAndIncludeInternalTransactionsParams() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        MoralisTransaction transaction = new MoralisTransaction("hash", null, null, null, null, null, null, null);
        when(moralisCallTemplate.get(eq("/transaction/0xhash"), captor.capture(), eq(MoralisTransaction.class)))
                .thenReturn(transaction);

        assertThat(api.transaction("0xhash", Chain.ETHEREUM)).isSameAs(transaction);

        UriBuilder uriBuilder = captureAndApply(captor.getValue());
        verify(uriBuilder).queryParam("chain", "0x1");
        verify(uriBuilder).queryParam("include", "internal_transactions");
    }

    @Test
    void walletHistoryAddsChainOrderLimitAndIncludeInternalParams() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        MoralisHistoryEnvelope envelope = new MoralisHistoryEnvelope(null, List.of());
        when(moralisCallTemplate.get(eq("/wallets/0xabc/history"), captor.capture(), eq(MoralisHistoryEnvelope.class)))
                .thenReturn(envelope);

        assertThat(api.walletHistory("0xabc", Chain.ETHEREUM)).isSameAs(envelope);

        UriBuilder uriBuilder = captureAndApply(captor.getValue());
        verify(uriBuilder).queryParam("chain", "0x1");
        verify(uriBuilder).queryParam("order", "DESC");
        verify(uriBuilder).queryParam("limit", 25);
        verify(uriBuilder).queryParam("include_internal_transactions", true);
        verify(moralisCallTemplate).require(envelope.result(), "/wallets/0xabc/history");
    }

    @Test
    void walletHistoryPropagatesExceptionWhenResultIsNull() {
        MoralisHistoryEnvelope envelope = new MoralisHistoryEnvelope(null, null);
        when(moralisCallTemplate.get(eq("/wallets/0xabc/history"), any(Consumer.class), eq(MoralisHistoryEnvelope.class)))
                .thenReturn(envelope);
        when(moralisCallTemplate.require(null, "/wallets/0xabc/history"))
                .thenThrow(new ChainDataException("no payload"));

        assertThatThrownBy(() -> api.walletHistory("0xabc", Chain.ETHEREUM))
                .isInstanceOf(ChainDataException.class);
    }

    @Test
    void walletActivityReturnsFirstActiveChainOnHappyPath() {
        MoralisActiveChain activeChain = new MoralisActiveChain("eth", "0x1", null, null);
        when(moralisCallTemplate.get(eq("/wallets/0xabc/chains"), any(Consumer.class), eq(MoralisWalletChainsEnvelope.class)))
                .thenReturn(new MoralisWalletChainsEnvelope("0xabc", List.of(activeChain)));

        assertThat(api.walletActivity("0xabc", Chain.ETHEREUM)).contains(activeChain);
    }

    @Test
    void walletActivityReturnsEmptyWhenNotFoundExceptionThrown() {
        when(moralisCallTemplate.get(eq("/wallets/0xabc/chains"), any(Consumer.class), eq(MoralisWalletChainsEnvelope.class)))
                .thenThrow(new ChainDataNotFoundException("not found"));

        assertThat(api.walletActivity("0xabc", Chain.ETHEREUM)).isEmpty();
    }

    @Test
    void walletActivityReturnsEmptyWhenActiveChainsIsNull() {
        when(moralisCallTemplate.get(eq("/wallets/0xabc/chains"), any(Consumer.class), eq(MoralisWalletChainsEnvelope.class)))
                .thenReturn(new MoralisWalletChainsEnvelope("0xabc", null));

        assertThat(api.walletActivity("0xabc", Chain.ETHEREUM)).isEmpty();
    }

    @Test
    void tokenBalancesAddsExcludeSpamAndExcludeNativeParams() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        List<MoralisTokenBalance> tokens = List.of(new MoralisTokenBalance("USDC", "10", 10.0));
        when(moralisCallTemplate.get(eq("/wallets/0xabc/tokens"), captor.capture(), eq(MoralisTokenBalancesEnvelope.class)))
                .thenReturn(new MoralisTokenBalancesEnvelope(tokens));

        assertThat(api.tokenBalances("0xabc", Chain.ETHEREUM)).isEqualTo(tokens);

        UriBuilder uriBuilder = captureAndApply(captor.getValue());
        verify(uriBuilder).queryParam("exclude_spam", true);
        verify(uriBuilder).queryParam("exclude_native", true);
    }

    @Test
    void chainHexThrowsChainDataExceptionForChainWithoutEvmChainId() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(moralisCallTemplate.get(eq("/0xabc/balance"), captor.capture(), eq(MoralisBalance.class)))
                .thenReturn(new MoralisBalance("0"));

        api.balanceNative("0xabc", Chain.BITCOIN);

        UriBuilder uriBuilder = mock(UriBuilder.class, RETURNS_SELF);
        assertThatThrownBy(() -> captor.getValue().accept(uriBuilder)).isInstanceOf(ChainDataException.class);
    }

    private static UriBuilder captureAndApply(Consumer<UriBuilder> consumer) {
        UriBuilder uriBuilder = mock(UriBuilder.class, RETURNS_SELF);
        consumer.accept(uriBuilder);
        return uriBuilder;
    }

    private static ChainIngestProperties.Provider provider(int pageSize) {
        return new ChainIngestProperties.Provider(null, null, pageSize, 0, 0, 0, null, null, null);
    }
}
