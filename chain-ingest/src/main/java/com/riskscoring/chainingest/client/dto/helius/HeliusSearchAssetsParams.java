package com.riskscoring.chainingest.client.dto.helius;

public record HeliusSearchAssetsParams(
        String ownerAddress,
        String tokenType,
        int limit,
        int page,
        HeliusDisplayOptions displayOptions
) {
}