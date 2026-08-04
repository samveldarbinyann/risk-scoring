package com.riskscoring.chainingest.client.dto.trongrid;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record TronAccount(
        long balance,
        @JsonProperty("create_time") long createTime,
        List<Map<String, String>> trc20
) {
}
