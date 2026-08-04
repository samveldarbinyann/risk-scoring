package com.riskscoring.chainingest.client.dto.trongrid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TronContractValue(
        @JsonProperty("owner_address") String ownerAddress,
        @JsonProperty("to_address") String toAddress,
        Long amount
) {
}
