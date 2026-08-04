package com.riskscoring.chainingest.client;

import com.riskscoring.chainingest.client.dto.trongrid.TronAccount;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransactionInfo;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;

import java.util.List;
import java.util.Optional;

public interface TronGridApi {

    Optional<TronAccount> account(String address);

    List<TronTransaction> accountTransactions(String address);

    List<TronTrc20Transfer> accountTrc20Transfers(String address);

    List<TronTrc20Transfer> trc20TransfersAround(String address, long blockTimestamp);

    TronTransaction transaction(String txId);

    TronTransactionInfo transactionInfo(String txId);
}
