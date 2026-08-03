package com.riskscoring.common.model;

import java.util.List;

public record AddressFacts(
        AddressSnapshot snapshot,
        List<Counterparty> counterparties
) implements ChainFacts {
}
