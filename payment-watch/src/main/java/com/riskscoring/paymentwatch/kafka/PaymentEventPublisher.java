package com.riskscoring.paymentwatch.kafka;

import com.riskscoring.common.event.UsdtPaymentDetected;

public interface PaymentEventPublisher {

    void publishUsdtPaymentDetected(UsdtPaymentDetected event);
}
