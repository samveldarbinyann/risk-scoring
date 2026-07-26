package com.riskscoring.chainingest.client.dto;

public record EtherscanTx(
        String timeStamp,
        String from,
        String to,
        String value,
        String contractAddress,
        String isError
) {
}
