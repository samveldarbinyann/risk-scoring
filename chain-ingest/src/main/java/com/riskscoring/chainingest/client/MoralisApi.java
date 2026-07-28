package com.riskscoring.chainingest.client;

import com.riskscoring.chainingest.client.dto.MoralisActiveChain;
import com.riskscoring.chainingest.client.dto.MoralisHistoryEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalancesEnvelope;

import java.util.Optional;

public interface MoralisApi {

    String balanceWei(String address, int chainId);

    MoralisHistoryEnvelope walletHistory(String address, int chainId);

    Optional<MoralisActiveChain> walletActivity(String address, int chainId);

    MoralisTokenBalancesEnvelope tokenBalances(String address, int chainId);
}
