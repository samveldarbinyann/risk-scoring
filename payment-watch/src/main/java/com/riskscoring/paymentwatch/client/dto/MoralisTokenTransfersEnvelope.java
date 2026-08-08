package com.riskscoring.paymentwatch.client.dto;

import java.util.List;

public record MoralisTokenTransfersEnvelope(
        String cursor,
        List<MoralisTokenTransfer> result
) {
}
