package com.riskscoring.chainingest.client.dto.helius;

import java.math.BigDecimal;

public record HeliusTokenTransfer(
        String fromUserAccount,
        String toUserAccount,
        String mint,
        BigDecimal tokenAmount
) {
}
