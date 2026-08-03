package com.riskscoring.chainingest.client;

import com.riskscoring.chainingest.client.dto.MoralisActiveChain;
import com.riskscoring.chainingest.client.dto.MoralisHistoryEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalance;
import com.riskscoring.chainingest.client.dto.MoralisTransaction;

import java.util.List;
import java.util.Optional;

public interface MoralisApi {

    String balanceWei(String address, int chainId);

    MoralisTransaction transaction(String hash, int chainId);

    MoralisHistoryEnvelope walletHistory(String address, int chainId);

    Optional<MoralisActiveChain> walletActivity(String address, int chainId);

    List<MoralisTokenBalance> tokenBalances(String address, int chainId);
}
