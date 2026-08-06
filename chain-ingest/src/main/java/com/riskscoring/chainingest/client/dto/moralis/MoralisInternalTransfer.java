package com.riskscoring.chainingest.client.dto.moralis;

public record MoralisInternalTransfer(
        String from,
        String to,
        String value
) {
}
