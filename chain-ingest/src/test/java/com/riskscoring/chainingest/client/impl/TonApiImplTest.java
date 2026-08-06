package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.dto.tonapi.TonAccount;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvents;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonBalance;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonsBalances;
import com.riskscoring.chainingest.config.ChainIngestProperties;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class TonApiImplTest {

    @Mock
    private HttpCallTemplate tonApiCallTemplate;

    @Mock
    private ChainIngestProperties properties;

    private TonApiImpl api;

    @BeforeEach
    void setUp() {
        api = new TonApiImpl(tonApiCallTemplate, properties);
    }

    @Test
    void accountCallsExpectedPath() {
        TonAccount account = new TonAccount(1000L);
        when(tonApiCallTemplate.get("/v2/accounts/addr", TonAccount.class)).thenReturn(account);

        assertThat(api.account("addr")).isSameAs(account);
    }

    @Test
    void accountEventsAddsLimitParamAndDefaultsNullEventsToEmptyList() {
        lenient().when(properties.tonApi()).thenReturn(provider(20));
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(tonApiCallTemplate.get(eq("/v2/accounts/addr/events"), captor.capture(), eq(TonEvents.class)))
                .thenReturn(new TonEvents(null));

        assertThat(api.accountEvents("addr")).isEmpty();
        UriBuilder uriBuilder = mock(UriBuilder.class, RETURNS_SELF);
        captor.getValue().accept(uriBuilder);
        verify(uriBuilder).queryParam("limit", 20);
    }

    @Test
    void firstEventUsesLimitOneAndAscendingSortOrder() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        TonEvent event = new TonEvent("evt", 1L, false, List.of());
        when(tonApiCallTemplate.get(eq("/v2/accounts/addr/events"), captor.capture(), eq(TonEvents.class)))
                .thenReturn(new TonEvents(List.of(event)));

        assertThat(api.firstEvent("addr")).contains(event);
        UriBuilder uriBuilder = mock(UriBuilder.class, RETURNS_SELF);
        captor.getValue().accept(uriBuilder);
        verify(uriBuilder).queryParam("limit", 1);
        verify(uriBuilder).queryParam("sort_order", "asc");
    }

    @Test
    void firstEventReturnsEmptyWhenNoEvents() {
        when(tonApiCallTemplate.get(eq("/v2/accounts/addr/events"), any(Consumer.class), eq(TonEvents.class)))
                .thenReturn(new TonEvents(List.of()));

        assertThat(api.firstEvent("addr")).isEmpty();
    }

    @Test
    void jettonsAddsCurrenciesParamAndDefaultsNullBalancesToEmptyList() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(tonApiCallTemplate.get(eq("/v2/accounts/addr/jettons"), captor.capture(), eq(TonJettonsBalances.class)))
                .thenReturn(new TonJettonsBalances(null));

        assertThat(api.jettons("addr")).isEmpty();
        UriBuilder uriBuilder = mock(UriBuilder.class, RETURNS_SELF);
        captor.getValue().accept(uriBuilder);
        verify(uriBuilder).queryParam("currencies", "usd");
    }

    @Test
    void jettonsReturnsBalancesWhenPresent() {
        TonJettonBalance balance = new TonJettonBalance("100", null, null);
        when(tonApiCallTemplate.get(eq("/v2/accounts/addr/jettons"), any(Consumer.class), eq(TonJettonsBalances.class)))
                .thenReturn(new TonJettonsBalances(List.of(balance)));

        assertThat(api.jettons("addr")).containsExactly(balance);
    }

    @Test
    void eventCallsExpectedPathByHash() {
        TonEvent event = new TonEvent("evt", 1L, false, List.of());
        when(tonApiCallTemplate.get("/v2/events/evt-hash", TonEvent.class)).thenReturn(event);

        assertThat(api.event("evt-hash")).isSameAs(event);
    }

    private static ChainIngestProperties.Provider provider(int pageSize) {
        return new ChainIngestProperties.Provider(null, null, pageSize, 0, 0, 0, null, null, null);
    }
}
