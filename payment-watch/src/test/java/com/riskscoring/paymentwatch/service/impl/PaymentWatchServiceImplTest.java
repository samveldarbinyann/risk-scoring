package com.riskscoring.paymentwatch.service.impl;

import com.riskscoring.common.event.UsdtPaymentDetected;
import com.riskscoring.common.model.Chain;
import com.riskscoring.paymentwatch.client.MoralisPaymentClient;
import com.riskscoring.paymentwatch.client.dto.MoralisTokenTransfer;
import com.riskscoring.paymentwatch.config.PaymentWatchProperties;
import com.riskscoring.paymentwatch.entity.EmittedTransfer;
import com.riskscoring.paymentwatch.kafka.PaymentEventPublisher;
import com.riskscoring.paymentwatch.repository.EmittedTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWatchServiceImplTest {

    private static final String TARGET_ADDRESS = "0xTestPaymentAddress";
    private static final String OTHER_ADDRESS = "0xSomeoneElse";
    private static final String USDT_CONTRACT = "0x55d398326f99059fF775485246999027B3197955";

    @Mock
    private MoralisPaymentClient moralisPaymentClient;

    @Mock
    private EmittedTransferRepository emittedTransferRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Captor
    private ArgumentCaptor<UsdtPaymentDetected> eventCaptor;

    @Captor
    private ArgumentCaptor<EmittedTransfer> emittedCaptor;

    private PaymentWatchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentWatchServiceImpl(
                moralisPaymentClient, emittedTransferRepository, paymentEventPublisher, properties(TARGET_ADDRESS));
    }

    @Test
    void pollForPaymentsSkipsWorkWhenTargetAddressIsNotConfigured() {
        PaymentWatchServiceImpl unconfigured = new PaymentWatchServiceImpl(
                moralisPaymentClient, emittedTransferRepository, paymentEventPublisher, properties(""));

        unconfigured.pollForPayments();

        verify(moralisPaymentClient, never()).incomingUsdtTransfers(any());
    }

    @Test
    void pollForPaymentsEmitsAConfirmedNewIncomingTransfer() {
        Instant blockTimestamp = Instant.now().minus(Duration.ofSeconds(60));
        MoralisTokenTransfer transfer = transfer("0xtx1", TARGET_ADDRESS, "20.004137", blockTimestamp, 12345L);
        when(moralisPaymentClient.incomingUsdtTransfers(any())).thenReturn(List.of(transfer));
        when(emittedTransferRepository.existsById("0xtx1")).thenReturn(false);

        service.pollForPayments();

        verify(emittedTransferRepository).save(emittedCaptor.capture());
        assertThat(emittedCaptor.getValue().getTxHash()).isEqualTo("0xtx1");

        verify(paymentEventPublisher).publishUsdtPaymentDetected(eventCaptor.capture());
        UsdtPaymentDetected event = eventCaptor.getValue();
        assertThat(event.txHash()).isEqualTo("0xtx1");
        assertThat(event.toAddress()).isEqualTo(TARGET_ADDRESS);
        assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("20.004137"));
        assertThat(event.chain()).isEqualTo(Chain.BNB_SMART_CHAIN);
        assertThat(event.blockNumber()).isEqualTo(12345L);
        assertThat(event.blockTimestamp()).isEqualTo(blockTimestamp);
    }

    @Test
    void pollForPaymentsIgnoresTransfersToADifferentAddress() {
        MoralisTokenTransfer transfer = transfer(
                "0xtx1", OTHER_ADDRESS, "5.000000", Instant.now().minus(Duration.ofSeconds(60)), 1L);
        when(moralisPaymentClient.incomingUsdtTransfers(any())).thenReturn(List.of(transfer));

        service.pollForPayments();

        verify(paymentEventPublisher, never()).publishUsdtPaymentDetected(any());
        verify(emittedTransferRepository, never()).save(any());
    }

    @Test
    void pollForPaymentsSkipsTransfersThatHaveNotClearedTheConfirmationWindowYet() {
        MoralisTokenTransfer transfer = transfer(
                "0xtx1", TARGET_ADDRESS, "5.000000", Instant.now().minus(Duration.ofSeconds(5)), 1L);
        when(moralisPaymentClient.incomingUsdtTransfers(any())).thenReturn(List.of(transfer));

        service.pollForPayments();

        verify(paymentEventPublisher, never()).publishUsdtPaymentDetected(any());
        verify(emittedTransferRepository, never()).save(any());
    }

    @Test
    void pollForPaymentsSkipsAlreadyEmittedTransfers() {
        MoralisTokenTransfer transfer = transfer(
                "0xtx1", TARGET_ADDRESS, "5.000000", Instant.now().minus(Duration.ofSeconds(60)), 1L);
        when(moralisPaymentClient.incomingUsdtTransfers(any())).thenReturn(List.of(transfer));
        when(emittedTransferRepository.existsById("0xtx1")).thenReturn(true);

        service.pollForPayments();

        verify(paymentEventPublisher, never()).publishUsdtPaymentDetected(any());
        verify(emittedTransferRepository, never()).save(any());
    }

    private static MoralisTokenTransfer transfer(
            String txHash, String toAddress, String valueDecimal, Instant blockTimestamp, long blockNumber) {
        return new MoralisTokenTransfer(
                txHash, Long.toString(blockNumber), blockTimestamp.toString(), USDT_CONTRACT,
                "0xpayer", toAddress, valueDecimal);
    }

    private static PaymentWatchProperties properties(String targetAddress) {
        return new PaymentWatchProperties(
                targetAddress,
                Chain.BNB_SMART_CHAIN,
                USDT_CONTRACT,
                Duration.ofSeconds(30),
                Duration.ofSeconds(45),
                Duration.ofMinutes(30),
                new PaymentWatchProperties.Moralis(
                        "https://deep-index.moralis.io/api/v2.2", "test-key", 2, 3,
                        Duration.ofSeconds(2), Duration.ofSeconds(5), Duration.ofSeconds(10)));
    }
}
