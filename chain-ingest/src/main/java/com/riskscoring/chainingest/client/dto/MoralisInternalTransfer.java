package com.riskscoring.chainingest.client.dto;

public record MoralisInternalTransfer(
        String from,
        String to,
        String value
) {
}
