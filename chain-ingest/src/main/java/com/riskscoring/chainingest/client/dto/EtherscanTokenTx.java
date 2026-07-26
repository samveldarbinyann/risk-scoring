package com.riskscoring.chainingest.client.dto;

public record EtherscanTokenTx(
        String timeStamp,
        String from,
        String to,
        String tokenSymbol
) {
}
