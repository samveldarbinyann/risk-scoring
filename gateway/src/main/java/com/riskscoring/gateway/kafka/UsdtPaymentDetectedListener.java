package com.riskscoring.gateway.kafka;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.UsdtPaymentDetected;
import com.riskscoring.gateway.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UsdtPaymentDetectedListener {

    private final BillingService billingService;

    @KafkaListener(topics = Topics.USDT_PAYMENT_DETECTED)
    public void onUsdtPaymentDetected(UsdtPaymentDetected event) {
        log.info("Received {} txHash={} amount={}", Topics.USDT_PAYMENT_DETECTED, event.txHash(), event.amount());
        billingService.confirmPaymentFromChain(event);
    }
}
