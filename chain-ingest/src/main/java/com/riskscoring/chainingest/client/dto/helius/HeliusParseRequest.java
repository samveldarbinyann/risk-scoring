package com.riskscoring.chainingest.client.dto.helius;

import java.util.List;

public record HeliusParseRequest(
        List<String> transactions
) {
}
