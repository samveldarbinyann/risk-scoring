package com.riskscoring.chainingest.client.dto.trongrid;

import java.util.List;

public record TronAccountResponse(
        List<TronAccount> data
) {
}
