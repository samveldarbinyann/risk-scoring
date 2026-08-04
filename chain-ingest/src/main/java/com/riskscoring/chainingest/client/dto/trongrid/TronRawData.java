package com.riskscoring.chainingest.client.dto.trongrid;

import java.util.List;

public record TronRawData(
        List<TronContract> contract
) {
}
