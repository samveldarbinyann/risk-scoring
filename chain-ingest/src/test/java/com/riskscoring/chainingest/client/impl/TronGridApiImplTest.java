package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.dto.trongrid.TronAccount;
import com.riskscoring.chainingest.client.dto.trongrid.TronAccountResponse;
import com.riskscoring.chainingest.client.dto.trongrid.TronRawData;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransactionInfo;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransactionRequest;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransactionsResponse;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Response;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
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
class TronGridApiImplTest {

    @Mock
    private HttpCallTemplate tronGridCallTemplate;

    @Mock
    private ChainIngestProperties properties;

    private TronGridApiImpl api;

    @BeforeEach
    void setUp() {
        api = new TronGridApiImpl(tronGridCallTemplate, properties);
        lenient().when(properties.tronGrid()).thenReturn(provider(30));
    }

    @Test
    void accountReturnsEmptyWhenDataIsNullOrEmpty() {
        when(tronGridCallTemplate.get("/v1/accounts/addr", TronAccountResponse.class))
                .thenReturn(new TronAccountResponse(null));

        assertThat(api.account("addr")).isEmpty();
    }

    @Test
    void accountReturnsFirstElementWhenPresent() {
        TronAccount account = new TronAccount(100L, 1L, null);
        when(tronGridCallTemplate.get("/v1/accounts/addr", TronAccountResponse.class))
                .thenReturn(new TronAccountResponse(List.of(account)));

        assertThat(api.account("addr")).contains(account);
    }

    @Test
    void accountTransactionsAddsLimitParamAndDefaultsNullDataToEmptyList() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(tronGridCallTemplate.get(eq("/v1/accounts/addr/transactions"), captor.capture(), eq(TronTransactionsResponse.class)))
                .thenReturn(new TronTransactionsResponse(null));

        assertThat(api.accountTransactions("addr")).isEmpty();
        UriBuilder uriBuilder = mock(UriBuilder.class, RETURNS_SELF);
        captor.getValue().accept(uriBuilder);
        verify(uriBuilder).queryParam("limit", 30);
    }

    @Test
    void accountTrc20TransfersAddsLimitParamAndDefaultsNullDataToEmptyList() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(tronGridCallTemplate.get(eq("/v1/accounts/addr/transactions/trc20"), captor.capture(), eq(TronTrc20Response.class)))
                .thenReturn(new TronTrc20Response(null));

        assertThat(api.accountTrc20Transfers("addr")).isEmpty();
        UriBuilder uriBuilder = mock(UriBuilder.class, RETURNS_SELF);
        captor.getValue().accept(uriBuilder);
        verify(uriBuilder).queryParam("limit", 30);
    }

    @Test
    void trc20TransfersAtSetsMinAndMaxTimestampToBlockTimestamp() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        TronTrc20Transfer transfer = new TronTrc20Transfer("tx", 500L, "from", "to", "1", null);
        when(tronGridCallTemplate.get(eq("/v1/accounts/addr/transactions/trc20"), captor.capture(), eq(TronTrc20Response.class)))
                .thenReturn(new TronTrc20Response(List.of(transfer)));

        assertThat(api.trc20TransfersAt("addr", 12345L)).containsExactly(transfer);
        UriBuilder uriBuilder = mock(UriBuilder.class, RETURNS_SELF);
        captor.getValue().accept(uriBuilder);
        verify(uriBuilder).queryParam("limit", 30);
        verify(uriBuilder).queryParam("min_timestamp", 12345L);
        verify(uriBuilder).queryParam("max_timestamp", 12345L);
    }

    @Test
    void transactionPostsTransactionRequestAndThrowsNotFoundWhenRawDataMissing() {
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        when(tronGridCallTemplate.post(eq("/wallet/gettransactionbyid"), bodyCaptor.capture(), eq(TronTransaction.class)))
                .thenReturn(new TronTransaction("txid", 0L, null, null));

        assertThatThrownBy(() -> api.transaction("txid")).isInstanceOf(ChainDataNotFoundException.class);
        assertThat(bodyCaptor.getValue()).isEqualTo(new TronTransactionRequest("txid", true));
    }

    @Test
    void transactionReturnsTransactionWhenRawDataPresent() {
        TronTransaction transaction = new TronTransaction("txid", 0L, null, new TronRawData(List.of()));
        when(tronGridCallTemplate.post(eq("/wallet/gettransactionbyid"), any(), eq(TronTransaction.class)))
                .thenReturn(transaction);

        assertThat(api.transaction("txid")).isSameAs(transaction);
    }

    @Test
    void transactionInfoPostsTransactionRequest() {
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        TronTransactionInfo info = new TronTransactionInfo(123L);
        when(tronGridCallTemplate.post(eq("/wallet/gettransactioninfobyid"), bodyCaptor.capture(), eq(TronTransactionInfo.class)))
                .thenReturn(info);

        assertThat(api.transactionInfo("txid")).isSameAs(info);
        assertThat(bodyCaptor.getValue()).isEqualTo(new TronTransactionRequest("txid", true));
    }

    private static ChainIngestProperties.Provider provider(int pageSize) {
        return new ChainIngestProperties.Provider(null, null, pageSize, 0, 0, 0, null, null, null);
    }
}
