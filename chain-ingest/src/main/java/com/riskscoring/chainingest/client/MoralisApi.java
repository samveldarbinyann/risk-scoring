package com.riskscoring.chainingest.client;

import com.riskscoring.chainingest.client.dto.MoralisActiveChain;
import com.riskscoring.chainingest.client.dto.MoralisHistoryEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalance;
import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.common.model.Chain;

import java.util.List;
import java.util.Optional;

public interface MoralisApi {

    String balanceNative(String address, Chain chain);

    MoralisTransaction transaction(String hash, Chain chain);

    MoralisHistoryEnvelope walletHistory(String address, Chain chain);

    Optional<MoralisActiveChain> walletActivity(String address, Chain chain);

    List<MoralisTokenBalance> tokenBalances(String address, Chain chain);
}
