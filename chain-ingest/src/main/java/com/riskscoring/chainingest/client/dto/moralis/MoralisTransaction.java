package com.riskscoring.chainingest.client.dto.moralis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MoralisTransaction(
        String hash,
        @JsonProperty("from_address") String fromAddress,
        @JsonProperty("to_address") String toAddress,
        String value,
        @JsonProperty("block_timestamp") String blockTimestamp,
        @JsonProperty("receipt_status") String receiptStatus,
        @JsonProperty("internal_transactions") List<MoralisInternalTransfer> internalTransactions,
        @JsonProperty("erc20_transfers") List<MoralisErc20Transfer> erc20Transfers
) {
}
