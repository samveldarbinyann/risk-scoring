package com.riskscoring.chainingest.client.dto.tonapi;

import java.util.List;

public record TonJettonsBalances(
        List<TonJettonBalance> balances
) {
}
