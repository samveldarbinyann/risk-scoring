package com.riskscoring.chainingest.client.dto.trongrid;

public record TronTokenInfo(
        String symbol,
        String address,
        int decimals,
        String name
) {
}
