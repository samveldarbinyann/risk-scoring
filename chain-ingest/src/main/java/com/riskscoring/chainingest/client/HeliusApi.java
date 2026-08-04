package com.riskscoring.chainingest.client;

import com.riskscoring.chainingest.client.dto.helius.HeliusPortfolio;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;

import java.util.List;

public interface HeliusApi {

    List<HeliusTransaction> addressTransactions(String address);

    HeliusTransaction transaction(String signature);

    HeliusPortfolio portfolio(String address);
}