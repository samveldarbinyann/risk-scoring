package com.riskscoring.paymentwatch.client;

import com.riskscoring.paymentwatch.client.dto.MoralisTokenTransfer;

import java.time.Instant;
import java.util.List;

public interface MoralisPaymentClient {

    List<MoralisTokenTransfer> incomingUsdtTransfers(Instant since);
}
