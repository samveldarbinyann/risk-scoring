package com.riskscoring.chainingest.client.dto.helius;

public record HeliusPortfolioResponse(
        HeliusPortfolio result,
        HeliusRpcError error
) {
}
