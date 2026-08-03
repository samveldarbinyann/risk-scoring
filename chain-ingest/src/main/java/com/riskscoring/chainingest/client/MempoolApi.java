package com.riskscoring.chainingest.client;

import com.riskscoring.chainingest.client.dto.mempool.MempoolAddressStats;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;

import java.util.List;

public interface MempoolApi {

    MempoolAddressStats addressStats(String address);

    List<MempoolTransaction> addressTransactions(String address);

    MempoolTransaction transaction(String txid);
}
