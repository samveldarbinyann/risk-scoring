package com.riskscoring.common.event;

import com.riskscoring.common.model.Chain;

import java.math.BigDecimal;
import java.time.Instant;

public record UsdtPaymentDetected(
        String txHash,
        String toAddress,
        BigDecimal amount,
        Chain chain,
        long blockNumber,
        Instant blockTimestamp,
        Instant detectedAt
) {
}
