package com.riskscoring.paymentwatch.service.impl;

import com.riskscoring.common.event.UsdtPaymentDetected;
import com.riskscoring.paymentwatch.client.MoralisPaymentClient;
import com.riskscoring.paymentwatch.client.dto.MoralisTokenTransfer;
import com.riskscoring.paymentwatch.config.PaymentWatchProperties;
import com.riskscoring.paymentwatch.entity.EmittedTransfer;
import com.riskscoring.paymentwatch.kafka.PaymentEventPublisher;
import com.riskscoring.paymentwatch.repository.EmittedTransferRepository;
import com.riskscoring.paymentwatch.service.PaymentWatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWatchServiceImpl implements PaymentWatchService {

    private final MoralisPaymentClient moralisPaymentClient;
    private final EmittedTransferRepository emittedTransferRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentWatchProperties properties;

    @Override
    @Scheduled(fixedDelayString = "${payment-watch.poll-interval}")
    @Transactional
    public void pollForPayments() {
        if (properties.targetAddress() == null || properties.targetAddress().isBlank()) {
            log.debug("payment-watch.target-address is not configured, skipping poll");
            return;
        }

        Instant now = Instant.now();
        Instant since = now.minus(properties.lookbackWindow());
        Instant confirmationCutoff = now.minus(properties.confirmationWindow());

        List<MoralisTokenTransfer> transfers = moralisPaymentClient.incomingUsdtTransfers(since);
        for (MoralisTokenTransfer transfer : transfers) {
            processTransfer(transfer, confirmationCutoff);
        }
    }

    private void processTransfer(MoralisTokenTransfer transfer, Instant confirmationCutoff) {
        if (!properties.targetAddress().equalsIgnoreCase(transfer.toAddress())) {
            return;
        }

        Instant blockTimestamp = Instant.parse(transfer.blockTimestamp());
        if (blockTimestamp.isAfter(confirmationCutoff)) {
            // Not enough confirmations yet — a later tick will pick it up once it clears the window.
            return;
        }

        if (emittedTransferRepository.existsById(transfer.transactionHash())) {
            return;
        }

        emittedTransferRepository.save(EmittedTransfer.builder()
                .txHash(transfer.transactionHash())
                .emittedAt(Instant.now())
                .build());

        UsdtPaymentDetected event = new UsdtPaymentDetected(
                transfer.transactionHash(),
                transfer.toAddress(),
                new BigDecimal(transfer.valueDecimal()),
                properties.chain(),
                Long.parseLong(transfer.blockNumber()),
                blockTimestamp,
                Instant.now());

        paymentEventPublisher.publishUsdtPaymentDetected(event);
        log.info("Detected incoming USDT payment txHash={} amount={}", transfer.transactionHash(), event.amount());
    }
}
