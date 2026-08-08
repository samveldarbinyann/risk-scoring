package com.riskscoring.paymentwatch.client.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.paymentwatch.client.HttpCallTemplate;
import com.riskscoring.paymentwatch.client.dto.MoralisTokenTransfer;
import com.riskscoring.paymentwatch.client.dto.MoralisTokenTransfersEnvelope;
import com.riskscoring.paymentwatch.config.PaymentWatchProperties;
import com.riskscoring.paymentwatch.exception.PaymentWatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.UriBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class MoralisPaymentClientImplTest {

    private static final String TARGET_ADDRESS = "0xTestPaymentAddress";
    private static final String USDT_CONTRACT = "0x55d398326f99059fF775485246999027B3197955";
    private static final String PATH = "/" + TARGET_ADDRESS + "/erc20/transfers";

    @Mock
    private HttpCallTemplate moralisCallTemplate;

    @Mock
    private PaymentWatchProperties properties;

    private MoralisPaymentClientImpl client;

    @BeforeEach
    void setUp() {
        client = new MoralisPaymentClientImpl(moralisCallTemplate, properties);
        lenient().when(properties.targetAddress()).thenReturn(TARGET_ADDRESS);
        lenient().when(properties.usdtContractAddress()).thenReturn(USDT_CONTRACT);
        lenient().when(properties.chain()).thenReturn(Chain.BNB_SMART_CHAIN);
        lenient().when(moralisCallTemplate.require(any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void incomingUsdtTransfersReturnsResultOfSinglePage() {
        MoralisTokenTransfer transfer = transfer("0xtx1");
        when(moralisCallTemplate.get(eq(PATH), any(Consumer.class), eq(MoralisTokenTransfersEnvelope.class)))
                .thenReturn(new MoralisTokenTransfersEnvelope(null, List.of(transfer)));

        List<MoralisTokenTransfer> result = client.incomingUsdtTransfers(Instant.now().minus(Duration.ofMinutes(30)));

        assertThat(result).containsExactly(transfer);
    }

    @Test
    void incomingUsdtTransfersFollowsCursorAcrossPagesUntilExhausted() {
        MoralisTokenTransfer firstPageTransfer = transfer("0xtx1");
        MoralisTokenTransfer secondPageTransfer = transfer("0xtx2");

        when(moralisCallTemplate.get(eq(PATH), any(Consumer.class), eq(MoralisTokenTransfersEnvelope.class)))
                .thenReturn(new MoralisTokenTransfersEnvelope("cursor-1", List.of(firstPageTransfer)))
                .thenReturn(new MoralisTokenTransfersEnvelope(null, List.of(secondPageTransfer)));

        List<MoralisTokenTransfer> result = client.incomingUsdtTransfers(Instant.now().minus(Duration.ofMinutes(30)));

        assertThat(result).containsExactly(firstPageTransfer, secondPageTransfer);
    }

    @Test
    void incomingUsdtTransfersSendsCursorQueryParamOnFollowUpPage() {
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(moralisCallTemplate.get(eq(PATH), captor.capture(), eq(MoralisTokenTransfersEnvelope.class)))
                .thenReturn(new MoralisTokenTransfersEnvelope("cursor-1", List.of(transfer("0xtx1"))))
                .thenReturn(new MoralisTokenTransfersEnvelope(null, List.of(transfer("0xtx2"))));

        client.incomingUsdtTransfers(Instant.now().minus(Duration.ofMinutes(30)));

        UriBuilder secondPageBuilder = mock(UriBuilder.class, RETURNS_SELF);
        captor.getAllValues().get(1).accept(secondPageBuilder);
        org.mockito.Mockito.verify(secondPageBuilder).queryParam("cursor", "cursor-1");
    }

    @Test
    void chainHexThrowsWhenChainHasNoEvmChainId() {
        when(properties.chain()).thenReturn(Chain.BITCOIN);
        ArgumentCaptor<Consumer<UriBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(moralisCallTemplate.get(eq(PATH), captor.capture(), eq(MoralisTokenTransfersEnvelope.class)))
                .thenReturn(new MoralisTokenTransfersEnvelope(null, List.of()));

        client.incomingUsdtTransfers(Instant.now().minus(Duration.ofMinutes(30)));

        UriBuilder uriBuilder = mock(UriBuilder.class, RETURNS_SELF);
        assertThatThrownBy(() -> captor.getValue().accept(uriBuilder)).isInstanceOf(PaymentWatchException.class);
    }

    private static MoralisTokenTransfer transfer(String txHash) {
        return new MoralisTokenTransfer(
                txHash, "1", Instant.now().toString(), USDT_CONTRACT, "0xpayer", TARGET_ADDRESS, "5.000000");
    }
}
