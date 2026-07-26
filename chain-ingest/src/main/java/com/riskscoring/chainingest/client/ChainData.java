package com.riskscoring.chainingest.client;

import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Counterparty;

import java.util.List;

public record ChainData(
        AddressSnapshot snapshot,
        List<Counterparty> counterparties
) {
}