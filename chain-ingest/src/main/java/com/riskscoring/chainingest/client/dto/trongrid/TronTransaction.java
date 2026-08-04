package com.riskscoring.chainingest.client.dto.trongrid;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TronTransaction(
        String txID,
        @JsonProperty("block_timestamp") long blockTimestamp,
        List<TronRet> ret,
        @JsonProperty("raw_data") TronRawData rawData
) {
}
