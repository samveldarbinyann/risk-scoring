package com.riskscoring.chainingest.client.dto.helius;

import java.util.List;

public record HeliusPortfolio(
        List<HeliusAsset> items,
        HeliusNativeBalance nativeBalance
) {
}