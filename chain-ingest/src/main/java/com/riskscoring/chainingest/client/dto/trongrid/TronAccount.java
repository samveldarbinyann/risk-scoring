package com.riskscoring.chainingest.client.dto.trongrid;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record TronAccount(
        Long balance,
        @JsonProperty("create_time") Long createTime,
        List<Map<String, String>> trc20
) {
}
