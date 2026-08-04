package com.riskscoring.chainingest.client.dto.trongrid;

import java.util.List;

public record TronTrc20Response(
        List<TronTrc20Transfer> data
) {
}
