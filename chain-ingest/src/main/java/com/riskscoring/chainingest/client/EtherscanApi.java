package com.riskscoring.chainingest.client;

import com.riskscoring.chainingest.client.dto.EtherscanTokenTx;
import com.riskscoring.chainingest.client.dto.EtherscanTx;

import java.util.List;
import java.util.Optional;

public interface EtherscanApi {

    String balanceWei(String address, int chainId);

    List<EtherscanTx> latestTransactions(String address, int chainId);

    List<EtherscanTx> latestInternalTransactions(String address, int chainId);

    List<EtherscanTokenTx> latestTokenTransfers(String address, int chainId);

    Optional<EtherscanTx> firstTransaction(String address, int chainId);
}
