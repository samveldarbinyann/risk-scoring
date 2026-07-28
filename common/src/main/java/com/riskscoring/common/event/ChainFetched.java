package com.riskscoring.common.event;

import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.Language;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChainFetched(
        UUID scanId,
        String address,
        int chainId,
        AddressSnapshot snapshot,
        List<Counterparty> counterparties,
        Language language,
        Instant fetchedAt
) {
}
