package com.riskscoring.chainingest.service;

import com.riskscoring.chainingest.client.ChainData;

import java.util.Optional;

public interface AddressCacheService {

    Optional<ChainData> findFresh(String address, int chainId);

    void store(String address, int chainId, ChainData chainData);
}
