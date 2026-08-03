package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import com.riskscoring.common.model.TransferDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class BitcoinTransferMapper {

    private final BitcoinValues values;

    public List<Transfer> fromTransactions(List<MempoolTransaction> transactions, String address) {
        return transactions.stream()
                .flatMap(transaction -> counterparties(transaction, address))
                .toList();
    }

    private Stream<Transfer> counterparties(MempoolTransaction transaction, String address) {
        Instant at = values.timestamp(transaction.status());

        return spends(transaction, address)
                ? recipients(transaction, address, at)
                : senders(transaction, address, at);
    }

    private boolean spends(MempoolTransaction transaction, String address) {
        return values.inputs(transaction).stream()
                .anyMatch(input -> address.equals(values.inputAddress(input)));
    }

    private Stream<Transfer> recipients(MempoolTransaction transaction, String address, Instant at) {
        return values.outputs(transaction).stream()
                .map(output -> new Transfer(
                        values.normalize(output.address()), TransferDirection.OUT, BigInteger.valueOf(output.value()), at))
                .filter(transfer -> isOther(transfer, address));
    }

    private Stream<Transfer> senders(MempoolTransaction transaction, String address, Instant at) {
        return values.inputs(transaction).stream()
                .map(input -> new Transfer(
                        values.inputAddress(input),
                        TransferDirection.IN,
                        BigInteger.valueOf(values.inputValue(input)),
                        at))
                .filter(transfer -> isOther(transfer, address));
    }

    private boolean isOther(Transfer transfer, String address) {
        return values.isRoutable(transfer.counterparty()) && !address.equals(transfer.counterparty());
    }
}
