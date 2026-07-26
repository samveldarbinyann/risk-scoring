package com.riskscoring.chainingest.client;

public interface ChainDataClient {

    ChainData fetch(String address, int chainId);
}