package com.riskscoring.paymentwatch.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.UsdtPaymentDetected;
import com.riskscoring.paymentwatch.kafka.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisherImpl implements PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishUsdtPaymentDetected(UsdtPaymentDetected event) {
        kafkaTemplate.send(Topics.USDT_PAYMENT_DETECTED, event.txHash(), event);
        log.info("Published {} txHash={} amount={}", Topics.USDT_PAYMENT_DETECTED, event.txHash(), event.amount());
    }
}
