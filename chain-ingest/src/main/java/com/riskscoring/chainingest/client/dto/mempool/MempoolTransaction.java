package com.riskscoring.chainingest.client.dto.mempool;

import java.util.List;

public record MempoolTransaction(
        String txid,
        long fee,
        List<MempoolVin> vin,
        List<MempoolVout> vout,
        MempoolStatus status
) {
}
