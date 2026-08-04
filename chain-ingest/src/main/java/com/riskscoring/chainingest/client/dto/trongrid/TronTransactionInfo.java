package com.riskscoring.chainingest.client.dto.trongrid;

public record TronTransactionInfo(
        String id,
        long blockTimeStamp,
        TronReceipt receipt
) {
}
